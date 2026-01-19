#!/usr/bin/env python3
"""
CodeWiki Data Analyzer
Provides analysis functions for module trees and dependency graphs.
"""

import json
from typing import Dict, List, Set, Tuple, Any, Optional
from collections import defaultdict, deque
from pathlib import Path


class CodeWikiAnalyzer:
    """Main analyzer class for CodeWiki documentation generation."""
    
    def __init__(self, module_tree_path: str, dependency_graph_path: str):
        """Initialize analyzer with input files."""
        self.module_tree = self._load_json(module_tree_path)
        self.dependency_graph = self._load_json(dependency_graph_path)
        
        # Parsed structures
        self.parsed_tree = None
        self.component_map = None
        self.reverse_deps = None
        
        # Parse immediately
        self._parse()
    
    def _load_json(self, filepath: str) -> Dict:
        """Load and parse JSON file."""
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
    
    def _parse(self):
        """Parse all input data."""
        self.parsed_tree = self.parse_module_tree()
        self.component_map = self.build_component_map()
        self.reverse_deps = self._build_reverse_dependencies()
    
    # ===== MODULE TREE PARSING =====
    
    def parse_module_tree(self) -> Dict:
        """Parse module tree into structured format."""
        result = {
            'modules': {},
            'leaf_modules': [],
            'parent_modules': [],
            'root_modules': [],
            'component_to_module': {}
        }
        
        def traverse(modules_dict, parent_path=None, level=0):
            for module_path, module_data in modules_dict.items():
                # Store module info
                is_leaf = len(module_data.get('children', {})) == 0
                
                result['modules'][module_path] = {
                    'path': module_path,
                    'components': module_data.get('components', []),
                    'children': module_data.get('children', {}),
                    'parent': parent_path,
                    'is_leaf': is_leaf,
                    'level': level
                }
                
                # Categorize
                if is_leaf:
                    result['leaf_modules'].append(module_path)
                else:
                    result['parent_modules'].append(module_path)
                
                if parent_path is None:
                    result['root_modules'].append(module_path)
                
                # Map components to modules
                for component_id in module_data.get('components', []):
                    result['component_to_module'][component_id] = module_path
                
                # Recurse into children
                if module_data.get('children'):
                    traverse(module_data['children'], module_path, level + 1)
        
        traverse(self.module_tree)
        return result
    
    def get_processing_order(self) -> List[List[str]]:
        """Get bottom-up processing order for modules."""
        if not self.parsed_tree:
            self.parsed_tree = self.parse_module_tree()
        
        # Group by level, sorted ascending
        max_level = max(m['level'] for m in self.parsed_tree['modules'].values())
        
        order = []
        for level in range(max_level, -1, -1):
            level_modules = [
                path for path, info in self.parsed_tree['modules'].items()
                if info['level'] == level
            ]
            if level_modules:
                order.append(level_modules)
        
        return order
    
    # ===== DEPENDENCY GRAPH ANALYSIS =====
    
    def build_component_map(self) -> Dict:
        """Build comprehensive component information map."""
        comp_map = {}
        
        for comp_id, comp_data in self.dependency_graph.items():
            comp_map[comp_id] = {
                'id': comp_data.get('id', comp_id),
                'name': comp_data.get('name', comp_id.split('.')[-1]),
                'component_type': comp_data.get('component_type', 'unknown'),
                'file_path': comp_data.get('file_path', ''),
                'relative_path': comp_data.get('relative_path', ''),
                'depends_on': comp_data.get('depends_on', []),
                'depended_by': [],  # Will be filled by reverse lookup
                'module': None,  # Will be filled from module tree
                'dependency_count': len(comp_data.get('depends_on', [])),
                'dependent_count': 0  # Will be calculated
            }
        
        # Fill module assignment
        if self.parsed_tree:
            for comp_id in comp_map:
                if comp_id in self.parsed_tree['component_to_module']:
                    comp_map[comp_id]['module'] = self.parsed_tree['component_to_module'][comp_id]
        
        return comp_map
    
    def _build_reverse_dependencies(self) -> Dict[str, List[str]]:
        """Build reverse dependency map (who depends on each component)."""
        reverse = defaultdict(list)
        
        for comp_id, comp_info in self.component_map.items():
            for dep_id in comp_info['depends_on']:
                reverse[dep_id].append(comp_id)
        
        # Update component map with reverse deps
        for comp_id, dependents in reverse.items():
            if comp_id in self.component_map:
                self.component_map[comp_id]['depended_by'] = dependents
                self.component_map[comp_id]['dependent_count'] = len(dependents)
        
        return dict(reverse)
    
    def analyze_dependencies(self, component_id: str) -> Dict:
        """Analyze dependencies for a specific component."""
        if component_id not in self.component_map:
            return {
                'error': f'Component {component_id} not found',
                'internal_dependencies': [],
                'external_dependencies': [],
                'internal_dependents': [],
                'external_dependents': [],
                'dependency_modules': [],
                'dependent_modules': []
            }
        
        comp_info = self.component_map[component_id]
        comp_module = comp_info['module']
        
        internal_deps = []
        external_deps = []
        internal_dependents = []
        external_dependents = []
        dep_modules = set()
        dependent_modules = set()
        
        # Analyze dependencies
        for dep_id in comp_info['depends_on']:
            if dep_id in self.component_map:
                dep_comp = self.component_map[dep_id]
                dep_info = {
                    'id': dep_id,
                    'name': dep_comp['name'],
                    'type': dep_comp['component_type']
                }
                
                if dep_comp['module'] == comp_module:
                    internal_deps.append(dep_info)
                else:
                    dep_info['module'] = dep_comp['module']
                    external_deps.append(dep_info)
                    if dep_comp['module']:
                        dep_modules.add(dep_comp['module'])
        
        # Analyze dependents
        for dependent_id in comp_info['depended_by']:
            if dependent_id in self.component_map:
                dependent_comp = self.component_map[dependent_id]
                dependent_info = {
                    'id': dependent_id,
                    'name': dependent_comp['name'],
                    'type': dependent_comp['component_type']
                }
                
                if dependent_comp['module'] == comp_module:
                    internal_dependents.append(dependent_info)
                else:
                    dependent_info['module'] = dependent_comp['module']
                    external_dependents.append(dependent_info)
                    if dependent_comp['module']:
                        dependent_modules.add(dependent_comp['module'])
        
        return {
            'internal_dependencies': internal_deps,
            'external_dependencies': external_deps,
            'internal_dependents': internal_dependents,
            'external_dependents': external_dependents,
            'dependency_modules': sorted(dep_modules),
            'dependent_modules': sorted(dependent_modules)
        }
    
    # ===== MODULE-LEVEL ANALYSIS =====
    
    def analyze_module_dependencies(self, module_path: str) -> Dict:
        """Analyze dependencies at module level."""
        if module_path not in self.parsed_tree['modules']:
            return {'error': f'Module {module_path} not found'}
        
        module_info = self.parsed_tree['modules'][module_path]
        components = module_info['components']
        
        internal_deps = []
        external_deps_by_module = defaultdict(list)
        external_dependents_by_module = defaultdict(list)
        
        for comp_id in components:
            if comp_id not in self.component_map:
                continue
            
            comp_info = self.component_map[comp_id]
            
            # Internal dependencies
            for dep_id in comp_info['depends_on']:
                if dep_id in self.component_map:
                    dep_module = self.component_map[dep_id]['module']
                    
                    if dep_module == module_path:
                        internal_deps.append({
                            'from': comp_id,
                            'to': dep_id,
                            'type': 'depends_on'
                        })
                    elif dep_module:
                        external_deps_by_module[dep_module].append({
                            'from_component': comp_id,
                            'to_component': dep_id
                        })
            
            # External dependents
            for dependent_id in comp_info['depended_by']:
                if dependent_id in self.component_map:
                    dependent_module = self.component_map[dependent_id]['module']
                    
                    if dependent_module and dependent_module != module_path:
                        external_dependents_by_module[dependent_module].append({
                            'from_component': dependent_id,
                            'to_component': comp_id
                        })
        
        # Format external dependencies
        external_deps = [
            {
                'target_module': mod,
                'relationships': rels
            }
            for mod, rels in external_deps_by_module.items()
        ]
        
        external_dependents = [
            {
                'source_module': mod,
                'relationships': rels
            }
            for mod, rels in external_dependents_by_module.items()
        ]
        
        # Calculate complexity metrics
        external_edge_count = sum(len(d['relationships']) for d in external_deps)
        internal_edge_count = len(internal_deps)
        total_edges = internal_edge_count + external_edge_count
        
        complexity = {
            'component_count': len(components),
            'internal_edge_count': internal_edge_count,
            'external_edge_count': external_edge_count,
            'cohesion_score': internal_edge_count / total_edges if total_edges > 0 else 0.0
        }
        
        return {
            'module': module_path,
            'components': components,
            'internal_dependencies': internal_deps,
            'external_dependencies': external_deps,
            'external_dependents': external_dependents,
            'complexity': complexity
        }
    
    # ===== PATTERN DETECTION =====
    
    def infer_component_purpose(self, component_id: str) -> Dict:
        """Infer the purpose of a component."""
        if component_id not in self.component_map:
            return {'error': f'Component {component_id} not found'}
        
        comp_info = self.component_map[component_id]
        name = comp_info['name']
        comp_type = comp_info['component_type']
        dep_count = comp_info['dependency_count']
        dependent_count = comp_info['dependent_count']
        
        # Infer role from name patterns
        role = 'unknown'
        confidence = 0.5
        reasoning = []
        
        name_lower = name.lower()
        
        # Name-based role detection
        if 'manager' in name_lower:
            role = 'manager'
            confidence = 0.8
            reasoning.append('Name contains "manager" - likely manages resources or lifecycle')
        elif 'service' in name_lower:
            role = 'service'
            confidence = 0.8
            reasoning.append('Name contains "service" - likely provides business logic')
        elif 'generator' in name_lower or 'builder' in name_lower:
            role = 'generator'
            confidence = 0.8
            reasoning.append('Name suggests object creation or construction')
        elif 'analyzer' in name_lower or 'parser' in name_lower:
            role = 'analyzer'
            confidence = 0.8
            reasoning.append('Name suggests data analysis or transformation')
        elif 'handler' in name_lower or 'processor' in name_lower:
            role = 'processor'
            confidence = 0.7
            reasoning.append('Name suggests event handling or data processing')
        elif 'adapter' in name_lower or 'wrapper' in name_lower:
            role = 'adapter'
            confidence = 0.8
            reasoning.append('Name suggests interface adaptation or wrapping')
        elif 'model' in name_lower or 'entity' in name_lower or 'dto' in name_lower:
            role = 'model'
            confidence = 0.7
            reasoning.append('Name suggests data structure or model')
        elif 'util' in name_lower or 'helper' in name_lower:
            role = 'utility'
            confidence = 0.7
            reasoning.append('Name suggests utility or helper functions')
        elif 'config' in name_lower or 'settings' in name_lower:
            role = 'configuration'
            confidence = 0.8
            reasoning.append('Name suggests configuration management')
        
        # Dependency pattern analysis
        if dep_count == 0:
            if role == 'unknown':
                role = 'model'
                confidence = 0.6
            reasoning.append('No dependencies - likely a data model, constant, or utility')
        elif dep_count > 10:
            if role == 'unknown':
                role = 'controller'
                confidence = 0.6
            reasoning.append(f'Many dependencies ({dep_count}) - likely an orchestrator or controller')
        
        if dependent_count == 0:
            reasoning.append('No dependents - possibly an entry point or unused component')
        elif dependent_count > 10:
            reasoning.append(f'Many dependents ({dependent_count}) - likely a core/shared component')
        
        # Type-based inference
        if comp_type == 'function':
            reasoning.append('Component is a function - likely a utility or specific operation')
        elif comp_type == 'class':
            reasoning.append('Component is a class - encapsulates state and behavior')
        
        # Generate purpose description
        purpose = self._generate_purpose_description(name, role, comp_type, dep_count, dependent_count)
        
        return {
            'primary_purpose': purpose,
            'role': role,
            'confidence': confidence,
            'reasoning': reasoning,
            'metrics': {
                'dependencies': dep_count,
                'dependents': dependent_count,
                'type': comp_type
            }
        }
    
    def _generate_purpose_description(self, name: str, role: str, 
                                      comp_type: str, dep_count: int, 
                                      dependent_count: int) -> str:
        """Generate a concise purpose description."""
        role_descriptions = {
            'manager': f'{name} manages lifecycle and resources',
            'service': f'{name} provides business logic and operations',
            'generator': f'{name} creates or constructs objects/data',
            'analyzer': f'{name} analyzes or transforms data',
            'processor': f'{name} processes data or handles events',
            'adapter': f'{name} adapts or wraps external interfaces',
            'model': f'{name} represents data structure or entity',
            'utility': f'{name} provides helper functions',
            'configuration': f'{name} manages configuration settings',
            'controller': f'{name} orchestrates operations',
            'unknown': f'{name} ({comp_type})'
        }
        
        return role_descriptions.get(role, role_descriptions['unknown'])
    
    def detect_module_patterns(self, module_path: str) -> Dict:
        """Detect architectural patterns in a module."""
        module_analysis = self.analyze_module_dependencies(module_path)
        
        if 'error' in module_analysis:
            return module_analysis
        
        patterns = []
        component_roles = {}
        
        components = module_analysis['components']
        
        # Analyze each component
        for comp_id in components:
            purpose = self.infer_component_purpose(comp_id)
            if 'error' not in purpose:
                component_roles[comp_id] = {
                    'role': purpose['role'],
                    'confidence': purpose['confidence'],
                    'reasoning': purpose['reasoning'][0] if purpose['reasoning'] else ''
                }
        
        # Detect layered architecture
        if self._is_layered_architecture(module_analysis, component_roles):
            patterns.append({
                'type': 'layered',
                'confidence': 0.7,
                'evidence': ['Clear separation of concerns', 'Unidirectional dependencies'],
                'components': components
            })
        
        # Detect plugin pattern
        plugin_comps = [c for c, r in component_roles.items() 
                       if 'plugin' in self.component_map[c]['name'].lower()]
        if len(plugin_comps) >= 2:
            patterns.append({
                'type': 'plugin',
                'confidence': 0.8,
                'evidence': [f'Multiple plugin-like components: {len(plugin_comps)}'],
                'components': plugin_comps
            })
        
        # Detect facade pattern
        facade_comps = self._detect_facade_components(module_analysis, component_roles)
        if facade_comps:
            patterns.append({
                'type': 'facade',
                'confidence': 0.7,
                'evidence': ['Components with high external fan-in and internal fan-out'],
                'components': facade_comps
            })
        
        return {
            'patterns': patterns,
            'component_roles': component_roles
        }
    
    def _is_layered_architecture(self, module_analysis: Dict, 
                                  component_roles: Dict) -> bool:
        """Check if module exhibits layered architecture."""
        # Simple heuristic: look for clear role separation
        roles = [r['role'] for r in component_roles.values()]
        role_counts = defaultdict(int)
        for role in roles:
            role_counts[role] += 1
        
        # If we have distinct role groups, might be layered
        return len(role_counts) >= 3 and max(role_counts.values()) >= 2
    
    def _detect_facade_components(self, module_analysis: Dict, 
                                   component_roles: Dict) -> List[str]:
        """Detect components acting as facades."""
        facades = []
        
        for comp_id in module_analysis['components']:
            if comp_id not in self.component_map:
                continue
            
            comp_info = self.component_map[comp_id]
            
            # High external dependent count (many use it from outside)
            external_dependents = len([
                d for d in comp_info['depended_by']
                if self.component_map.get(d, {}).get('module') != module_analysis['module']
            ])
            
            # High internal dependency count (uses many internal components)
            internal_deps = len([
                d for d in comp_info['depends_on']
                if self.component_map.get(d, {}).get('module') == module_analysis['module']
            ])
            
            if external_dependents >= 3 and internal_deps >= 3:
                facades.append(comp_id)
        
        return facades
    
    # ===== REPORTING =====
    
    def generate_analysis_report(self, module_path: str) -> Dict:
        """Generate comprehensive analysis report for a module."""
        if module_path not in self.parsed_tree['modules']:
            return {'error': f'Module {module_path} not found'}
        
        module_info = self.parsed_tree['modules'][module_path]
        module_analysis = self.analyze_module_dependencies(module_path)
        patterns = self.detect_module_patterns(module_path)
        
        report = {
            'module': module_path,
            'summary': {
                'path': module_path,
                'component_count': len(module_info['components']),
                'is_leaf': module_info['is_leaf'],
                'level': module_info['level'],
                'has_children': len(module_info['children']) > 0
            },
            'dependencies': module_analysis['external_dependencies'],
            'dependents': module_analysis['external_dependents'],
            'complexity': module_analysis['complexity'],
            'patterns': patterns,
            'components': {}
        }
        
        # Detailed component analysis
        for comp_id in module_info['components']:
            if comp_id not in self.component_map:
                continue
            
            comp_deps = self.analyze_dependencies(comp_id)
            purpose = self.infer_component_purpose(comp_id)
            
            report['components'][comp_id] = {
                'info': self.component_map[comp_id],
                'dependencies': comp_deps,
                'purpose': purpose
            }
        
        return report
    
    def get_repository_summary(self) -> Dict:
        """Get high-level repository summary."""
        return {
            'total_modules': len(self.parsed_tree['modules']),
            'leaf_modules': len(self.parsed_tree['leaf_modules']),
            'parent_modules': len(self.parsed_tree['parent_modules']),
            'root_modules': len(self.parsed_tree['root_modules']),
            'total_components': len(self.component_map),
            'max_depth': max(m['level'] for m in self.parsed_tree['modules'].values()),
            'processing_order_levels': len(self.get_processing_order())
        }


def main():
    """Example usage."""
    import sys
    
    if len(sys.argv) < 3:
        print("Usage: python codewiki_analyzer.py <module_tree.json> <dependency_graph.json>")
        sys.exit(1)
    
    analyzer = CodeWikiAnalyzer(sys.argv[1], sys.argv[2])
    
    # Print repository summary
    print("=" * 60)
    print("REPOSITORY SUMMARY")
    print("=" * 60)
    summary = analyzer.get_repository_summary()
    for key, value in summary.items():
        print(f"{key}: {value}")
    
    print("\n" + "=" * 60)
    print("PROCESSING ORDER")
    print("=" * 60)
    order = analyzer.get_processing_order()
    for level, modules in enumerate(order):
        print(f"\nLevel {level}:")
        for module in modules:
            print(f"  - {module}")
    
    print("\n" + "=" * 60)
    print("LEAF MODULE ANALYSIS")
    print("=" * 60)
    for module_path in analyzer.parsed_tree['leaf_modules'][:2]:  # First 2 for demo
        print(f"\n### {module_path} ###")
        report = analyzer.generate_analysis_report(module_path)
        print(f"Components: {report['summary']['component_count']}")
        print(f"External Dependencies: {len(report['dependencies'])}")
        print(f"External Dependents: {len(report['dependents'])}")
        print(f"Detected Patterns: {len(report['patterns']['patterns'])}")


if __name__ == '__main__':
    main()
