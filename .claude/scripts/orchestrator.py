#!/usr/bin/env python3
"""
CodeWiki Documentation Orchestrator
Coordinates the entire documentation generation process.
"""

import json
import os
from pathlib import Path
from typing import Dict, List, Optional
from codewiki_analyzer import CodeWikiAnalyzer


class CodeWikiOrchestrator:
    """Orchestrates the CodeWiki documentation generation process."""
    
    def __init__(self, module_tree_path: str, dependency_graph_path: str, 
                 output_dir: str = "./docs"):
        """Initialize orchestrator."""
        self.analyzer = CodeWikiAnalyzer(module_tree_path, dependency_graph_path)
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # Create architecture directory for diagrams
        self.arch_dir = self.output_dir / "architecture"
        self.arch_dir.mkdir(parents=True, exist_ok=True)
        self.diagrams_dir = self.arch_dir / "diagrams"
        self.diagrams_dir.mkdir(parents=True, exist_ok=True)
        
        # Track generated docs
        self.generated_docs = {}
        
    def generate_all_documentation(self):
        """Generate complete documentation suite."""
        print("CodeWiki Documentation Generator")
        print("=" * 60)
        
        # Step 1: Analyze repository
        print("\nStep 1: Analyzing repository...")
        summary = self.analyzer.get_repository_summary()
        print(f"  Modules: {summary['total_modules']}")
        print(f"  Components: {summary['total_components']}")
        print(f"  Depth: {summary['max_depth']}")
        
        # Step 2: Get processing order
        print("\nStep 2: Determining processing order...")
        processing_order = self.analyzer.get_processing_order()
        print(f"  Levels to process: {len(processing_order)}")
        
        # Step 3: Generate comprehensive diagrams
        print("\nStep 3: Generating architecture diagrams...")
        self._generate_all_diagrams()
        print("  ✓ System architecture diagram")
        print("  ✓ Module dependency diagram")
        print("  ✓ Component overview diagram")
        print("  ✓ Data flow diagram")
        
        # Step 4: Generate leaf module documentation
        print("\nStep 4: Generating leaf module documentation...")
        leaf_count = 0
        for module_path in processing_order[0]:
            self._generate_leaf_module_doc(module_path)
            leaf_count += 1
            print(f"  [{leaf_count}/{len(processing_order[0])}] {module_path}")
        
        # Step 4: Generate parent module documentation
        if len(processing_order) > 1:
            print("\nStep 5: Generating parent module documentation...")
            parent_count = 0
            total_parents = sum(len(level) for level in processing_order[1:])
            
            for level_idx, level_modules in enumerate(processing_order[1:], 1):
                for module_path in level_modules:
                    self._generate_parent_module_doc(module_path)
                    parent_count += 1
                    print(f"  [{parent_count}/{total_parents}] Level {level_idx}: {module_path}")
        
        # Step 6: Generate architecture overview
        print("\nStep 6: Generating architecture documentation...")
        self._generate_architecture_doc()
        print("  ✓ Architecture overview")
        print("  ✓ Pattern analysis")
        
        # Step 7: Generate repository overview
        print("\nStep 7: Generating repository overview...")
        self._generate_repository_overview()
        print("  ✓ README.md generated")
        
        # Step 8: Generate index and navigation
        print("\nStep 8: Generating navigation...")
        self._generate_navigation()
        print("  ✓ Navigation generated")
        
        print("\n" + "=" * 60)
        print(f"Documentation generated in: {self.output_dir}")
        print(f"Total files: {len(self.generated_docs)}")
        print("=" * 60)
    
    def _generate_leaf_module_doc(self, module_path: str):
        """Generate documentation for a leaf module."""
        # Get analysis
        report = self.analyzer.generate_analysis_report(module_path)
        
        # Build prompt data
        prompt_data = self._build_leaf_module_prompt_data(report)
        
        # Generate markdown (this would call Claude in production)
        markdown = self._format_leaf_module_markdown(report, prompt_data)
        
        # Save
        output_path = self._get_module_doc_path(module_path)
        self._save_documentation(output_path, markdown)
        self.generated_docs[module_path] = output_path
    
    def _generate_parent_module_doc(self, module_path: str):
        """Generate documentation for a parent module."""
        # Get module info
        module_info = self.analyzer.parsed_tree['modules'][module_path]
        
        # Get child documentation
        child_docs = {}
        for child_path in self._get_child_modules(module_path):
            if child_path in self.generated_docs:
                child_docs[child_path] = self._load_documentation(
                    self.generated_docs[child_path]
                )
        
        # Build prompt data
        prompt_data = self._build_parent_module_prompt_data(
            module_path, module_info, child_docs
        )
        
        # Generate markdown
        markdown = self._format_parent_module_markdown(
            module_path, module_info, child_docs, prompt_data
        )
        
        # Save
        output_path = self._get_module_doc_path(module_path)
        self._save_documentation(output_path, markdown)
        self.generated_docs[module_path] = output_path
    
    def _generate_repository_overview(self):
        """Generate repository-level overview documentation."""
        summary = self.analyzer.get_repository_summary()
        
        # Get all module summaries
        module_summaries = {}
        for module_path in self.analyzer.parsed_tree['root_modules']:
            if module_path in self.generated_docs:
                module_summaries[module_path] = self._extract_module_summary(
                    self.generated_docs[module_path]
                )
        
        # Generate markdown
        markdown = self._format_repository_overview_markdown(
            summary, module_summaries
        )
        
        # Save
        output_path = self.output_dir / "README.md"
        self._save_documentation(output_path, markdown)
        self.generated_docs['_repository_overview'] = output_path
    
    def _generate_navigation(self):
        """Generate navigation index."""
        nav_content = "# Documentation Index\n\n"
        
        # Architecture section
        nav_content += "## Architecture\n\n"
        nav_content += "- [Architecture Overview](architecture/overview.md)\n"
        nav_content += "- [System Architecture Diagram](architecture/diagrams/system-architecture.mmd)\n"
        nav_content += "- [Module Dependencies](architecture/diagrams/module-dependencies.mmd)\n"
        nav_content += "- [Component Overview](architecture/diagrams/component-overview.mmd)\n"
        nav_content += "- [Data Flow](architecture/diagrams/data-flow.mmd)\n\n"
        
        nav_content += "## Module Documentation\n\n"
        
        # Build tree view
        for module_path in sorted(self.generated_docs.keys()):
            if module_path.startswith('_'):
                continue
            
            level = self.analyzer.parsed_tree['modules'][module_path]['level']
            indent = "  " * level
            rel_path = self._get_relative_path(
                self.output_dir / "INDEX.md",
                self.generated_docs[module_path]
            )
            
            nav_content += f"{indent}- [{module_path}]({rel_path})\n"
        
        # Save
        output_path = self.output_dir / "INDEX.md"
        self._save_documentation(output_path, nav_content)
    
    # ===== PROMPT DATA BUILDERS =====
    
    def _build_leaf_module_prompt_data(self, report: Dict) -> Dict:
        """Build data dictionary for leaf module prompt."""
        module_info = report['summary']
        
        # Component list
        component_list = []
        for comp_id in report['components']:
            comp_info = report['components'][comp_id]['info']
            purpose = report['components'][comp_id]['purpose']
            component_list.append(
                f"- **{comp_info['name']}** ({comp_info['component_type']}): "
                f"{purpose['primary_purpose']}"
            )
        
        # External dependencies
        ext_deps = []
        for dep in report['dependencies']:
            ext_deps.append(
                f"- **{dep['target_module']}** "
                f"({len(dep['relationships'])} relationships)"
            )
        
        # External dependents
        ext_dependents = []
        for dep in report['dependents']:
            ext_dependents.append(
                f"- **{dep['source_module']}** "
                f"({len(dep['relationships'])} relationships)"
            )
        
        return {
            'module_path': report['module'],
            'component_count': module_info['component_count'],
            'complexity_score': report['complexity']['internal_edge_count'] + 
                              report['complexity']['external_edge_count'],
            'cohesion': round(report['complexity']['cohesion_score'] * 100, 2),
            'component_list': '\n'.join(component_list),
            'external_dependencies': '\n'.join(ext_deps) if ext_deps else "None",
            'external_dependents': '\n'.join(ext_dependents) if ext_dependents else "None",
            'patterns': self._format_patterns(report['patterns'])
        }
    
    def _build_parent_module_prompt_data(self, module_path: str, 
                                        module_info: Dict, 
                                        child_docs: Dict) -> Dict:
        """Build data dictionary for parent module prompt."""
        child_list = []
        for child_path in self._get_child_modules(module_path):
            if child_path in child_docs:
                summary = self._extract_module_summary_from_content(
                    child_docs[child_path]
                )
                child_list.append(f"- **{child_path}**: {summary}")
        
        return {
            'module_path': module_path,
            'level': module_info['level'],
            'child_count': len(module_info['children']),
            'child_module_list': '\n'.join(child_list)
        }
    
    # ===== MARKDOWN FORMATTERS =====
    
    def _format_leaf_module_markdown(self, report: Dict, 
                                     prompt_data: Dict) -> str:
        """Format leaf module documentation as markdown."""
        md = f"# Module: {report['module']}\n\n"
        
        # Overview
        md += "## Overview\n\n"
        md += f"This module contains {report['summary']['component_count']} components "
        md += f"with a cohesion score of {prompt_data['cohesion']}%. "
        md += self._infer_module_purpose(report) + "\n\n"
        
        # Architecture
        md += "## Architecture\n\n"
        md += self._generate_architecture_description(report) + "\n\n"
        
        # Component Diagram
        md += "### Component Diagram\n\n"
        md += self._generate_component_diagram_enhanced(report) + "\n\n"
        
        # Dependency Diagram (if has external dependencies)
        if report.get('dependencies') or report.get('dependents'):
            md += "### Module Dependencies\n\n"
            md += self._generate_module_dependency_detail_diagram(report) + "\n\n"
        
        # Components
        md += "## Components\n\n"
        for comp_id, comp_data in report['components'].items():
            md += self._format_component_section(comp_id, comp_data)
        
        # Dependencies
        if report['dependencies']:
            md += "## External Dependencies\n\n"
            for dep in report['dependencies']:
                md += f"### {dep['target_module']}\n"
                md += f"- {len(dep['relationships'])} relationship(s)\n\n"
        
        # Dependents
        if report['dependents']:
            md += "## Used By\n\n"
            for dep in report['dependents']:
                md += f"### {dep['source_module']}\n"
                md += f"- {len(dep['relationships'])} relationship(s)\n\n"
        
        # Patterns
        if report['patterns']['patterns']:
            md += "## Architectural Patterns\n\n"
            for pattern in report['patterns']['patterns']:
                md += f"### {pattern['type'].title()} Pattern\n"
                md += f"- **Confidence**: {pattern['confidence'] * 100:.0f}%\n"
                md += f"- **Evidence**: {', '.join(pattern['evidence'])}\n\n"
        
        return md
    
    def _format_parent_module_markdown(self, module_path: str, 
                                      module_info: Dict,
                                      child_docs: Dict,
                                      prompt_data: Dict) -> str:
        """Format parent module documentation as markdown."""
        md = f"# Module: {module_path}\n\n"
        
        # Overview
        md += "## Overview\n\n"
        md += f"This is a parent module containing {len(module_info['children'])} submodules. "
        md += self._infer_parent_module_purpose(module_path, child_docs) + "\n\n"
        
        # Architecture
        md += "## Architecture\n\n"
        md += self._generate_parent_architecture_description(
            module_path, child_docs
        ) + "\n\n"
        md += self._generate_module_hierarchy_diagram(module_path) + "\n\n"
        
        # Submodules
        md += "## Submodules\n\n"
        for child_path in self._get_child_modules(module_path):
            if child_path in child_docs:
                summary = self._extract_module_summary_from_content(
                    child_docs[child_path]
                )
                rel_path = self._get_relative_path(
                    self._get_module_doc_path(module_path),
                    self.generated_docs[child_path]
                )
                md += f"### [{child_path}]({rel_path})\n"
                md += f"{summary}\n\n"
        
        return md
    
    def _format_repository_overview_markdown(self, summary: Dict,
                                           module_summaries: Dict) -> str:
        """Format repository overview documentation as markdown."""
        md = "# Repository Documentation\n\n"
        
        # Purpose (would need repo name from config or inference)
        md += "## Purpose\n\n"
        md += "This repository contains a modular software system organized into "
        md += f"{summary['total_modules']} modules with {summary['total_components']} components.\n\n"
        
        # Architecture Overview
        md += "## Architecture Overview\n\n"
        md += f"The system is organized in a {summary['max_depth']}-level hierarchy. "
        md += "For detailed architecture information and diagrams, see:\n\n"
        md += "- [Architecture Overview](architecture/overview.md)\n"
        md += "- [System Architecture Diagram](architecture/diagrams/system-architecture.mmd)\n"
        md += "- [Module Dependencies](architecture/diagrams/module-dependencies.mmd)\n\n"
        
        # Quick Architecture Diagram
        md += self._generate_repository_architecture_diagram() + "\n\n"
        
        # Module Structure
        md += "## Module Structure\n\n"
        for module_path, module_summary in module_summaries.items():
            if module_path in self.generated_docs:
                rel_path = self._get_relative_path(
                    self.output_dir / "README.md",
                    self.generated_docs[module_path]
                )
                md += f"### [{module_path}]({rel_path})\n"
                md += f"{module_summary}\n\n"
        
        # Getting Started
        md += "## Getting Started\n\n"
        md += "Start by exploring the root modules:\n\n"
        for module_path in self.analyzer.parsed_tree['root_modules']:
            if module_path in self.generated_docs:
                rel_path = self._get_relative_path(
                    self.output_dir / "README.md",
                    self.generated_docs[module_path]
                )
                md += f"- [{module_path}]({rel_path})\n"
        
        md += "\n## Navigation\n\n"
        md += "See [INDEX.md](INDEX.md) for complete documentation navigation.\n"
        
        return md
    
    # ===== HELPER METHODS =====
    
    def _infer_module_purpose(self, report: Dict) -> str:
        """Infer module purpose from components."""
        # Collect component roles
        roles = []
        for comp_data in report['components'].values():
            roles.append(comp_data['purpose']['role'])
        
        # Determine dominant pattern
        if 'analyzer' in roles or 'parser' in roles:
            return "It provides data analysis and parsing capabilities."
        elif 'service' in roles:
            return "It implements business logic and services."
        elif 'manager' in roles:
            return "It manages resources and orchestrates operations."
        elif 'generator' in roles:
            return "It generates or constructs data and objects."
        else:
            return "It provides core functionality for the system."
    
    def _infer_parent_module_purpose(self, module_path: str, 
                                    child_docs: Dict) -> str:
        """Infer parent module purpose from children."""
        # Simple heuristic based on module path
        if 'fe' in module_path.lower():
            return "It manages the frontend layer of the application."
        elif 'be' in module_path.lower():
            return "It manages the backend layer of the application."
        elif 'cli' in module_path.lower():
            return "It provides command-line interface functionality."
        else:
            return "It coordinates its submodules to provide system functionality."
    
    def _generate_architecture_description(self, report: Dict) -> str:
        """Generate architecture description for a module."""
        desc = "The module is organized with the following structure:\n\n"
        
        # Describe component relationships
        internal_count = report['complexity']['internal_edge_count']
        external_count = report['complexity']['external_edge_count']
        
        if internal_count > 0:
            desc += f"- **Internal Dependencies**: {internal_count} relationships between components\n"
        
        if external_count > 0:
            desc += f"- **External Dependencies**: {external_count} relationships with other modules\n"
        
        # Describe cohesion
        cohesion = report['complexity']['cohesion_score']
        if cohesion > 0.7:
            desc += "- **High cohesion**: Components are closely related\n"
        elif cohesion > 0.4:
            desc += "- **Moderate cohesion**: Components have some independence\n"
        else:
            desc += "- **Low cohesion**: Components are relatively independent\n"
        
        return desc
    
    def _generate_parent_architecture_description(self, module_path: str,
                                                 child_docs: Dict) -> str:
        """Generate architecture description for parent module."""
        desc = "The parent module coordinates the following submodules:\n\n"
        
        for child_path in self._get_child_modules(module_path):
            if child_path in child_docs:
                desc += f"- **{child_path}**: "
                desc += self._extract_module_summary_from_content(child_docs[child_path])
                desc += "\n"
        
        return desc
    
    def _generate_component_diagram(self, report: Dict) -> str:
        """Generate Mermaid diagram for components."""
        diagram = "```mermaid\ngraph LR\n"
        
        # Add nodes for each component
        comp_nodes = {}
        for idx, comp_id in enumerate(report['components']):
            node_id = f"C{idx}"
            comp_name = report['components'][comp_id]['info']['name']
            comp_nodes[comp_id] = node_id
            diagram += f"    {node_id}[{comp_name}]\n"
        
        # Add internal edges
        for dep in report.get('internal_dependencies', []):
            from_id = comp_nodes.get(dep['from'])
            to_id = comp_nodes.get(dep['to'])
            if from_id and to_id:
                diagram += f"    {from_id} --> {to_id}\n"
        
        # Style
        for node_id in comp_nodes.values():
            diagram += f"    style {node_id} fill:#e1f5ff\n"
        
        diagram += "```\n"
        return diagram
    
    def _generate_module_hierarchy_diagram(self, module_path: str) -> str:
        """Generate Mermaid diagram for module hierarchy."""
        diagram = "```mermaid\ngraph TB\n"
        
        module_info = self.analyzer.parsed_tree['modules'][module_path]
        
        # Parent node
        diagram += f"    P[{module_path}]\n"
        
        # Child nodes
        for idx, child_path in enumerate(self._get_child_modules(module_path)):
            child_id = f"C{idx}"
            diagram += f"    {child_id}[{child_path}]\n"
            diagram += f"    P --> {child_id}\n"
        
        diagram += "```\n"
        return diagram
    
    def _generate_repository_architecture_diagram(self) -> str:
        """Generate Mermaid diagram for repository architecture."""
        diagram = "```mermaid\ngraph TB\n"
        
        # Root modules
        for idx, module_path in enumerate(self.analyzer.parsed_tree['root_modules']):
            mod_id = f"M{idx}"
            diagram += f"    {mod_id}[{module_path}]\n"
        
        diagram += "```\n"
        return diagram
    
    def _format_component_section(self, comp_id: str, comp_data: Dict) -> str:
        """Format a component section."""
        info = comp_data['info']
        purpose = comp_data['purpose']
        deps = comp_data['dependencies']
        
        md = f"### {info['name']}\n\n"
        md += f"**Type**: {info['component_type']}\n"
        md += f"**File**: `{info['relative_path']}`\n\n"
        md += f"**Purpose**: {purpose['primary_purpose']}\n\n"
        
        if deps['internal_dependencies']:
            md += "**Internal Dependencies**:\n"
            for dep in deps['internal_dependencies']:
                md += f"- {dep['name']}\n"
            md += "\n"
        
        if deps['external_dependencies']:
            md += "**External Dependencies**:\n"
            for dep in deps['external_dependencies']:
                md += f"- {dep['name']} ({dep.get('module', 'unknown')})\n"
            md += "\n"
        
        return md
    
    def _format_patterns(self, patterns: Dict) -> str:
        """Format detected patterns."""
        if not patterns['patterns']:
            return "No specific patterns detected."
        
        result = []
        for pattern in patterns['patterns']:
            result.append(
                f"- **{pattern['type'].title()}**: "
                f"{pattern['confidence'] * 100:.0f}% confidence"
            )
        
        return '\n'.join(result)
    
    def _get_child_modules(self, module_path: str) -> List[str]:
        """Get direct children of a module."""
        module_info = self.analyzer.parsed_tree['modules'][module_path]
        return list(module_info['children'].keys())
    
    def _get_module_doc_path(self, module_path: str) -> Path:
        """Get output path for module documentation."""
        # Convert module path to file path
        path_parts = module_path.split('/')
        doc_path = self.output_dir / "modules"
        
        for part in path_parts:
            doc_path = doc_path / part
        
        doc_path.mkdir(parents=True, exist_ok=True)
        return doc_path / "README.md"
    
    def _get_relative_path(self, from_path: Path, to_path: Path) -> str:
        """Get relative path between two paths."""
        try:
            return os.path.relpath(to_path, from_path.parent)
        except:
            return str(to_path)
    
    def _save_documentation(self, path: Path, content: str):
        """Save documentation to file."""
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
    
    def _load_documentation(self, path: Path) -> str:
        """Load documentation from file."""
        with open(path, 'r', encoding='utf-8') as f:
            return f.read()
    
    def _extract_module_summary(self, doc_path: Path) -> str:
        """Extract summary from module documentation."""
        content = self._load_documentation(doc_path)
        return self._extract_module_summary_from_content(content)
    
    def _extract_module_summary_from_content(self, content: str) -> str:
        """Extract summary from documentation content."""

    # ========== DIAGRAM GENERATION METHODS ==========
    
    def _generate_all_diagrams(self):
        """Generate all architecture diagrams."""
        # 1. System Architecture Diagram
        system_diagram = self._generate_system_architecture_diagram()
        self._save_diagram("system-architecture.mmd", system_diagram)
        
        # 2. Module Dependency Diagram
        module_dep_diagram = self._generate_module_dependency_diagram()
        self._save_diagram("module-dependencies.mmd", module_dep_diagram)
        
        # 3. Component Overview Diagram
        component_diagram = self._generate_component_overview_diagram()
        self._save_diagram("component-overview.mmd", component_diagram)
        
        # 4. Data Flow Diagram
        dataflow_diagram = self._generate_dataflow_diagram()
        self._save_diagram("data-flow.mmd", dataflow_diagram)
    
    def _generate_system_architecture_diagram(self):
        """Generate comprehensive system architecture diagram."""
        lines = ["```mermaid", "graph TB"]
        lines.append("    classDef moduleStyle fill:#e1f5ff,stroke:#0288d1,stroke-width:2px")
        lines.append("    classDef rootStyle fill:#c8e6c9,stroke:#388e3c,stroke-width:3px")
        lines.append("")
        
        node_map = {}
        node_counter = [0]  # Use list to make it mutable
        
        # Add all modules with hierarchy
        for module_path in self.analyzer.parsed_tree['root_modules']:
            self._add_module_tree_nodes(lines, module_path, node_map, node_counter)
        
        # Apply styles
        for module_path in self.analyzer.parsed_tree['root_modules']:
            if module_path in node_map:
                lines.append(f"    class {node_map[module_path]} rootStyle")
        
        lines.append("```")
        return "\n".join(lines)
    
    def _add_module_tree_nodes(self, lines, module_path, node_map, counter, parent_id=None):
        """Recursively add module tree to diagram lines."""
        node_id = f"M{counter[0]}"
        node_map[module_path] = node_id
        counter[0] += 1
        
        module_name = module_path.split('/')[-1]
        if len(module_name) > 15:
            module_name = module_name[:12] + "..."
        lines.append(f'    {node_id}["{module_name}"]')
        
        if parent_id:
            lines.append(f"    {parent_id} --> {node_id}")
        
        module_info = self.analyzer.parsed_tree['modules'][module_path]
        for child_path in module_info['children'].keys():
            self._add_module_tree_nodes(lines, child_path, node_map, counter, node_id)
    
    def _generate_module_dependency_diagram(self):
        """Generate module dependency diagram showing inter-module dependencies."""
        lines = ["```mermaid", "graph LR"]
        lines.append("    classDef highDep fill:#ffcdd2,stroke:#c62828")
        lines.append("    classDef medDep fill:#fff9c4,stroke:#f9a825")
        lines.append("    classDef lowDep fill:#c8e6c9,stroke:#388e3c")
        lines.append("")
        
        node_map = {}
        dep_counts = {}
        
        for idx, module_path in enumerate(self.analyzer.parsed_tree['modules'].keys()):
            node_id = f"M{idx}"
            node_map[module_path] = node_id
            module_name = module_path.split('/')[-1]
            if len(module_name) > 12:
                module_name = module_name[:9] + "..."
            lines.append(f'    {node_id}["{module_name}"]')
            dep_counts[module_path] = 0
        
        edges_added = set()
        for module_path in list(self.analyzer.parsed_tree['modules'].keys())[:20]:
            try:
                report = self.analyzer.generate_analysis_report(module_path)
                for dep in report.get('dependencies', []):
                    target_module = dep['target_module']
                    if target_module in node_map:
                        edge = (module_path, target_module)
                        if edge not in edges_added:
                            from_id = node_map[module_path]
                            to_id = node_map[target_module]
                            count = len(dep['relationships'])
                            lines.append(f"    {from_id} -->|{count}| {to_id}")
                            dep_counts[target_module] += count
                            edges_added.add(edge)
            except:
                pass
        
        for module_path, count in dep_counts.items():
            if module_path in node_map:
                node_id = node_map[module_path]
                if count > 5:
                    lines.append(f"    class {node_id} highDep")
                elif count > 2:
                    lines.append(f"    class {node_id} medDep")
                else:
                    lines.append(f"    class {node_id} lowDep")
        
        lines.append("```")
        return "\n".join(lines)
    
    def _generate_component_overview_diagram(self):
        """Generate overview of all components grouped by type."""
        lines = ["```mermaid", "graph TB"]
        lines.append("    classDef classType fill:#bbdefb,stroke:#1976d2")
        lines.append("    classDef funcType fill:#c5e1a5,stroke:#689f38")
        lines.append("    classDef moduleType fill:#fff9c4,stroke:#f9a825")
        lines.append("")
        
        by_type = {}
        for comp_id, comp_data in list(self.analyzer.dependency_graph.items())[:100]:
            comp_type = comp_data.get('component_type', 'unknown')
            if comp_type not in by_type:
                by_type[comp_type] = []
            by_type[comp_type].append(comp_data.get('name', comp_id))
        
        for idx, (comp_type, components) in enumerate(by_type.items()):
            type_node = f"T{idx}"
            lines.append(f'    {type_node}["{comp_type.upper()}<br/>{len(components)} components"]')
            
            for cidx, comp_name in enumerate(components[:3]):
                comp_node = f"C{idx}_{cidx}"
                safe_name = comp_name.replace('[', '').replace(']', '').replace('"', "'")
                if len(safe_name) > 20:
                    safe_name = safe_name[:17] + "..."
                lines.append(f'    {comp_node}["{safe_name}"]')
                lines.append(f"    {type_node} --> {comp_node}")
                
                if comp_type == 'class':
                    lines.append(f"    class {comp_node} classType")
                elif comp_type == 'function':
                    lines.append(f"    class {comp_node} funcType")
                else:
                    lines.append(f"    class {comp_node} moduleType")
        
        lines.append("```")
        return "\n".join(lines)
    
    def _generate_dataflow_diagram(self):
        """Generate data flow diagram showing main data paths."""
        lines = ["```mermaid", "graph LR"]
        lines.append("    classDef dataSource fill:#e3f2fd,stroke:#1976d2")
        lines.append("    classDef processor fill:#fff3e0,stroke:#f57c00")
        lines.append("    classDef dataStore fill:#f3e5f5,stroke:#7b1fa2")
        lines.append("")
        
        sources = []
        processors = []
        stores = []
        
        for comp_id, comp_data in list(self.analyzer.dependency_graph.items())[:50]:
            name = comp_data.get('name', '').lower()
            
            if any(term in name for term in ['input', 'source', 'reader', 'loader', 'fetch']):
                sources.append((comp_id, comp_data))
            elif any(term in name for term in ['store', 'repository', 'database', 'cache', 'storage']):
                stores.append((comp_id, comp_data))
            elif any(term in name for term in ['processor', 'handler', 'service', 'manager', 'controller']):
                processors.append((comp_id, comp_data))
        
        for idx, (comp_id, comp_data) in enumerate(sources[:3]):
            node_id = f"S{idx}"
            name = comp_data.get('name', comp_id)
            if len(name) > 15:
                name = name[:12] + "..."
            lines.append(f'    {node_id}["{name}"]')
            lines.append(f"    class {node_id} dataSource")
        
        for idx, (comp_id, comp_data) in enumerate(processors[:3]):
            node_id = f"P{idx}"
            name = comp_data.get('name', comp_id)
            if len(name) > 15:
                name = name[:12] + "..."
            lines.append(f'    {node_id}["{name}"]')
            lines.append(f"    class {node_id} processor")
        
        for idx, (comp_id, comp_data) in enumerate(stores[:3]):
            node_id = f"D{idx}"
            name = comp_data.get('name', comp_id)
            if len(name) > 15:
                name = name[:12] + "..."
            lines.append(f'    {node_id}[("{name}")]')
            lines.append(f"    class {node_id} dataStore")
        
        if sources and processors:
            lines.append("    S0 -->|data| P0")
        if processors and stores:
            lines.append("    P0 -->|save| D0")
        if len(stores) > 1 and len(processors) > 1:
            lines.append("    D0 -->|load| P1")
        
        lines.append("```")
        return "\n".join(lines)
    
    def _generate_architecture_doc(self):
        """Generate architecture documentation."""
        md = "# Architecture Overview\n\n"
        
        md += "## System Architecture\n\n"
        md += "The following diagram shows the complete system architecture:\n\n"
        md += self._load_diagram("system-architecture.mmd") + "\n\n"
        
        md += "## Module Dependencies\n\n"
        md += "This diagram illustrates dependencies between modules:\n\n"
        md += self._load_diagram("module-dependencies.mmd") + "\n\n"
        
        md += "## Component Overview\n\n"
        md += "Components organized by type:\n\n"
        md += self._load_diagram("component-overview.mmd") + "\n\n"
        
        md += "## Data Flow\n\n"
        md += "Main data paths through the system:\n\n"
        md += self._load_diagram("data-flow.mmd") + "\n\n"
        
        md += "## Architectural Patterns\n\n"
        md += self._generate_pattern_analysis() + "\n"
        
        arch_doc_path = self.arch_dir / "overview.md"
        self._save_documentation(arch_doc_path, md)
    
    def _generate_pattern_analysis(self):
        """Generate pattern analysis across all modules."""
        all_patterns = {}
        
        for module_path in list(self.analyzer.parsed_tree['modules'].keys())[:20]:
            try:
                report = self.analyzer.generate_analysis_report(module_path)
                for pattern in report['patterns']['patterns']:
                    pattern_type = pattern['type']
                    if pattern_type not in all_patterns:
                        all_patterns[pattern_type] = {
                            'count': 0,
                            'modules': [],
                            'avg_confidence': 0
                        }
                    all_patterns[pattern_type]['count'] += 1
                    all_patterns[pattern_type]['modules'].append(module_path)
                    all_patterns[pattern_type]['avg_confidence'] += pattern['confidence']
            except:
                pass
        
        if not all_patterns:
            return "No specific architectural patterns detected across modules.\n"
        
        md = "Detected patterns across the codebase:\n\n"
        for pattern_type, data in all_patterns.items():
            avg_conf = (data['avg_confidence'] / data['count']) * 100
            md += f"### {pattern_type.title()} Pattern\n\n"
            md += f"- **Occurrences**: {data['count']} modules\n"
            md += f"- **Average Confidence**: {avg_conf:.0f}%\n"
            md += f"- **Found in**: {', '.join(data['modules'][:5])}"
            if len(data['modules']) > 5:
                md += f" and {len(data['modules']) - 5} more"
            md += "\n\n"
        
        return md
    
    def _generate_component_diagram_enhanced(self, report):
        """Generate enhanced Mermaid diagram for components with styling."""
        lines = ["```mermaid", "graph LR"]
        lines.append("    classDef classNode fill:#bbdefb,stroke:#1976d2,stroke-width:2px")
        lines.append("    classDef funcNode fill:#c5e1a5,stroke:#689f38,stroke-width:2px")
        lines.append("    classDef moduleNode fill:#fff9c4,stroke:#f9a825,stroke-width:2px")
        lines.append("")
        
        comp_nodes = {}
        for idx, comp_id in enumerate(list(report['components'].keys())[:20]):
            node_id = f"C{idx}"
            comp_name = report['components'][comp_id]['info']['name']
            comp_type = report['components'][comp_id]['info']['component_type']
            comp_nodes[comp_id] = (node_id, comp_type)
            
            if len(comp_name) > 20:
                comp_name = comp_name[:17] + "..."
            
            lines.append(f'    {node_id}["{comp_name}"]')
        
        edges_added = set()
        for dep in report.get('internal_dependencies', []):
            from_data = comp_nodes.get(dep['from'])
            to_data = comp_nodes.get(dep['to'])
            if from_data and to_data:
                from_id, _ = from_data
                to_id, _ = to_data
                edge = (from_id, to_id)
                if edge not in edges_added:
                    lines.append(f"    {from_id} --> {to_id}")
                    edges_added.add(edge)
        
        for comp_id, (node_id, comp_type) in comp_nodes.items():
            if comp_type == 'class':
                lines.append(f"    class {node_id} classNode")
            elif comp_type == 'function':
                lines.append(f"    class {node_id} funcNode")
            else:
                lines.append(f"    class {node_id} moduleNode")
        
        lines.append("```")
        return "\n".join(lines)
    
    def _generate_module_dependency_detail_diagram(self, report):
        """Generate detailed dependency diagram for a specific module."""
        lines = ["```mermaid", "graph TB"]
        lines.append("    classDef thisModule fill:#c8e6c9,stroke:#388e3c,stroke-width:3px")
        lines.append("    classDef depModule fill:#ffcdd2,stroke:#c62828")
        lines.append("    classDef userModule fill:#bbdefb,stroke:#1976d2")
        lines.append("")
        
        module_name = report['module'].split('/')[-1]
        if len(module_name) > 15:
            module_name = module_name[:12] + "..."
        lines.append(f'    THIS["{module_name}"]')
        lines.append("    class THIS thisModule")
        lines.append("")
        
        for idx, dep in enumerate(report.get('dependencies', [])[:5]):
            dep_id = f"DEP{idx}"
            dep_name = dep['target_module'].split('/')[-1]
            if len(dep_name) > 12:
                dep_name = dep_name[:9] + "..."
            lines.append(f'    {dep_id}["{dep_name}"]')
            lines.append(f"    THIS -->|uses| {dep_id}")
            lines.append(f"    class {dep_id} depModule")
        
        for idx, dep in enumerate(report.get('dependents', [])[:5]):
            user_id = f"USER{idx}"
            user_name = dep['source_module'].split('/')[-1]
            if len(user_name) > 12:
                user_name = user_name[:9] + "..."
            lines.append(f'    {user_id}["{user_name}"]')
            lines.append(f"    {user_id} -->|uses| THIS")
            lines.append(f"    class {user_id} userModule")
        
        lines.append("```")
        return "\n".join(lines)
    
    def _save_diagram(self, filename, content):
        """Save diagram to diagrams directory."""
        path = self.diagrams_dir / filename
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
    
    def _load_diagram(self, filename):
        """Load diagram from diagrams directory."""
        path = self.diagrams_dir / filename
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return f.read()
        except:
            return f"<!-- Diagram {filename} not found -->"


def main():
    """Main entry point."""
    import sys
    
    if len(sys.argv) < 3:
        print("Usage: python orchestrator.py <module_tree.json> <dependency_graph.json> [output_dir]")
        sys.exit(1)
    
    module_tree_path = sys.argv[1]
    dep_graph_path = sys.argv[2]
    output_dir = sys.argv[3] if len(sys.argv) > 3 else "./docs"
    
    orchestrator = CodeWikiOrchestrator(module_tree_path, dep_graph_path, output_dir)
    orchestrator.generate_all_documentation()


if __name__ == '__main__':
    main()
