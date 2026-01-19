#!/usr/bin/env python3
"""
CodeWiki Diagram Enhancements
Additional methods to enhance orchestrator.py with comprehensive diagram generation.

USAGE:
1. Add these methods to the CodeWikiOrchestrator class in orchestrator.py
2. Modify __init__ to create diagram directories
3. Call _generate_all_diagrams() in generate_all_documentation()
"""

# ========== ADD TO __init__ METHOD ==========
"""
Add after self.output_dir.mkdir(parents=True, exist_ok=True):

        # Create architecture directory for diagrams
        self.arch_dir = self.output_dir / "architecture"
        self.arch_dir.mkdir(parents=True, exist_ok=True)
        self.diagrams_dir = self.arch_dir / "diagrams"
        self.diagrams_dir.mkdir(parents=True, exist_ok=True)
"""

# ========== ADD TO generate_all_documentation() METHOD ==========
"""
Add after processing order step and before leaf module generation:

        # Step 3: Generate comprehensive diagrams
        print("\nStep 3: Generating architecture diagrams...")
        self._generate_all_diagrams()
        print("  ✓ System architecture diagram")
        print("  ✓ Module dependency diagram")
        print("  ✓ Component overview diagram")
        print("  ✓ Data flow diagram")
        
Then renumber subsequent steps (leaf becomes Step 4, etc.)

Add before repository overview:

        # Step 6: Generate architecture overview
        print("\nStep 6: Generating architecture documentation...")
        self._generate_architecture_doc()
        print("  ✓ Architecture overview")
        print("  ✓ Pattern analysis")
"""

# ========== NEW METHODS TO ADD TO CodeWikiOrchestrator CLASS ==========

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

# ========== ENHANCED INLINE DIAGRAMS FOR MODULES ==========

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

# ========== UPDATE _format_leaf_module_markdown ==========
"""
Replace the Architecture section with:

        # Architecture
        md += "## Architecture\\n\\n"
        md += self._generate_architecture_description(report) + "\\n\\n"
        
        # Component Diagram
        md += "### Component Diagram\\n\\n"
        md += self._generate_component_diagram_enhanced(report) + "\\n\\n"
        
        # Dependency Diagram (if has external dependencies)
        if report.get('dependencies') or report.get('dependents'):
            md += "### Module Dependencies\\n\\n"
            md += self._generate_module_dependency_detail_diagram(report) + "\\n\\n"
"""

# ========== UPDATE _generate_navigation ==========
"""
Add architecture section at the beginning:

        # Architecture section
        md += "### Architecture\\n\\n"
        md += "- [Architecture Overview](architecture/overview.md)\\n"
        md += "- [System Architecture Diagram](architecture/diagrams/system-architecture.mmd)\\n"
        md += "- [Module Dependencies](architecture/diagrams/module-dependencies.mmd)\\n"
        md += "- [Component Overview](architecture/diagrams/component-overview.mmd)\\n"
        md += "- [Data Flow](architecture/diagrams/data-flow.mmd)\\n\\n"
"""

# ========== UPDATE _format_repository_overview_markdown ==========
"""
Replace Architecture Overview section with:

        # Architecture Overview
        md += "## Architecture Overview\\n\\n"
        md += f"The system is organized in a {summary['max_depth']}-level hierarchy. "
        md += "For detailed architecture information and diagrams, see:\\n\\n"
        md += "- [Architecture Overview](architecture/overview.md)\\n"
        md += "- [System Architecture Diagram](architecture/diagrams/system-architecture.mmd)\\n"
        md += "- [Module Dependencies](architecture/diagrams/module-dependencies.mmd)\\n\\n"
        
        # Quick Architecture Diagram
        md += self._generate_repository_architecture_diagram() + "\\n\\n"

Add at end before return:

        md += "\\n## Navigation\\n\\n"
        md += "See [INDEX.md](INDEX.md) for complete documentation navigation.\\n"
"""

print("""
================================================================================
CodeWiki Diagram Enhancements
================================================================================

This file contains all the methods and modifications needed to add
comprehensive diagram generation to orchestrator.py.

IMPLEMENTATION STEPS:

1. Add the __init__ modifications to create diagram directories

2. Add all the new methods (from _generate_all_diagrams onwards) to the
   CodeWikiOrchestrator class

3. Modify generate_all_documentation() to call _generate_all_diagrams()

4. Update _format_leaf_module_markdown to use enhanced diagrams

5. Update _generate_navigation to include architecture links

6. Update _format_repository_overview_markdown to link to architecture docs

RESULT:
- 4 comprehensive architecture diagrams
- Enhanced component diagrams with styling
- Module dependency diagrams for each module
- Architecture documentation section
- Complete navigation with diagram links

Total: 20+ Mermaid diagrams for a typical project!
================================================================================
""")
