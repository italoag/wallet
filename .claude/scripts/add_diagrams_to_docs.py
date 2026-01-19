#!/usr/bin/env python3
"""
CodeWiki Diagram Generator - Standalone
Adiciona diagramas a documentação existente SEM reescrever o conteúdo.

USO:
    python3 add_diagrams_to_docs.py <module_tree.json> <dependency_graph.json> <docs_dir>

EXEMPLO:
    python3 add_diagrams_to_docs.py module_tree.json dependency_graph.json ./docs
"""

import json
import sys
from pathlib import Path
from typing import Dict, List, Tuple
import re


class DiagramGenerator:
    """Gera e injeta diagramas em documentação existente."""
    
    def __init__(self, module_tree_path: str, dependency_graph_path: str, docs_dir: str):
        """Initialize diagram generator."""
        self.docs_dir = Path(docs_dir)
        
        # Load analysis files
        print(f"Loading analysis files...")
        with open(module_tree_path, 'r') as f:
            self.module_tree = json.load(f)
        with open(dependency_graph_path, 'r') as f:
            self.dependency_graph = json.load(f)
        
        # Parse module tree
        self.parsed_tree = self._parse_module_tree()
        
        # Create architecture directory
        self.arch_dir = self.docs_dir / "architecture"
        self.arch_dir.mkdir(parents=True, exist_ok=True)
        self.diagrams_dir = self.arch_dir / "diagrams"
        self.diagrams_dir.mkdir(parents=True, exist_ok=True)
        
        print(f"✓ Loaded {len(self.parsed_tree['modules'])} modules")
        print(f"✓ Loaded {len(self.dependency_graph)} components")
    
    def _parse_module_tree(self) -> Dict:
        """Parse module tree structure."""
        modules = {}
        root_modules = []
        
        def traverse(node, path="", level=0, parent=None):
            current_path = f"{path}/{node['name']}" if path else node['name']
            
            modules[current_path] = {
                'name': node['name'],
                'level': level,
                'parent': parent,
                'children': {},
                'component_count': node.get('component_count', 0)
            }
            
            if parent is None:
                root_modules.append(current_path)
            
            for child in node.get('children', []):
                child_path = traverse(child, current_path, level + 1, current_path)
                modules[current_path]['children'][child_path] = True
            
            return current_path
        
        for root in self.module_tree.get('children', []):
            traverse(root)
        
        return {
            'modules': modules,
            'root_modules': root_modules
        }
    
    def generate_all_diagrams(self):
        """Generate all architecture diagrams."""
        print("\n" + "=" * 60)
        print("Generating Architecture Diagrams")
        print("=" * 60)
        
        # 1. System Architecture
        print("\n1. Generating system architecture diagram...")
        system_diagram = self._generate_system_architecture_diagram()
        self._save_diagram("system-architecture.mmd", system_diagram)
        print("   ✓ system-architecture.mmd")
        
        # 2. Module Dependencies
        print("2. Generating module dependency diagram...")
        module_dep_diagram = self._generate_module_dependency_diagram()
        self._save_diagram("module-dependencies.mmd", module_dep_diagram)
        print("   ✓ module-dependencies.mmd")
        
        # 3. Component Overview
        print("3. Generating component overview diagram...")
        component_diagram = self._generate_component_overview_diagram()
        self._save_diagram("component-overview.mmd", component_diagram)
        print("   ✓ component-overview.mmd")
        
        # 4. Data Flow
        print("4. Generating data flow diagram...")
        dataflow_diagram = self._generate_dataflow_diagram()
        self._save_diagram("data-flow.mmd", dataflow_diagram)
        print("   ✓ data-flow.mmd")
        
        # 5. Architecture Overview Document
        print("5. Generating architecture overview document...")
        self._generate_architecture_overview_doc()
        print("   ✓ architecture/overview.md")
        
        print("\n✓ All architecture diagrams generated!")
    
    def inject_diagrams_into_modules(self):
        """Inject diagrams into existing module documentation."""
        print("\n" + "=" * 60)
        print("Injecting Diagrams into Module Documentation")
        print("=" * 60)
        
        modules_dir = self.docs_dir / "modules"
        if not modules_dir.exists():
            print("⚠ No modules directory found, skipping...")
            return
        
        count = 0
        for module_path in self.parsed_tree['modules'].keys():
            doc_path = self._get_module_doc_path(module_path)
            if doc_path.exists():
                self._inject_diagram_into_module_doc(module_path, doc_path)
                count += 1
                print(f"   ✓ {module_path}")
        
        print(f"\n✓ Updated {count} module documents with diagrams!")
    
    def update_navigation(self):
        """Update INDEX.md with architecture links."""
        print("\n" + "=" * 60)
        print("Updating Navigation")
        print("=" * 60)
        
        index_path = self.docs_dir / "INDEX.md"
        if not index_path.exists():
            print("⚠ INDEX.md not found, creating new one...")
            content = "# Documentation Index\n\n"
        else:
            with open(index_path, 'r') as f:
                content = f.read()
        
        # Check if architecture section already exists
        if "### Architecture" in content or "## Architecture" in content:
            print("   ℹ Architecture section already exists, skipping...")
        else:
            # Add architecture section after "Quick Navigation" or at the start
            arch_section = """### Architecture

- [Architecture Overview](architecture/overview.md)
- [System Architecture Diagram](architecture/diagrams/system-architecture.mmd)
- [Module Dependencies](architecture/diagrams/module-dependencies.mmd)
- [Component Overview](architecture/diagrams/component-overview.mmd)
- [Data Flow](architecture/diagrams/data-flow.mmd)

"""
            
            if "## Quick Navigation" in content:
                content = content.replace(
                    "## Quick Navigation\n\n",
                    "## Quick Navigation\n\n" + arch_section
                )
            else:
                # Add after first heading
                lines = content.split('\n')
                for i, line in enumerate(lines):
                    if line.startswith('# '):
                        lines.insert(i + 2, arch_section)
                        break
                content = '\n'.join(lines)
            
            with open(index_path, 'w') as f:
                f.write(content)
            print("   ✓ INDEX.md updated")
    
    def update_readme(self):
        """Update README.md with architecture links."""
        print("Updating README...")
        
        readme_path = self.docs_dir / "README.md"
        if not readme_path.exists():
            print("   ⚠ README.md not found, skipping...")
            return
        
        with open(readme_path, 'r') as f:
            content = f.read()
        
        # Check if architecture links already exist
        if "architecture/overview.md" in content:
            print("   ℹ Architecture links already exist, skipping...")
            return
        
        # Add architecture section or update existing one
        arch_text = """For detailed architecture information and diagrams, see:

- [Architecture Overview](architecture/overview.md)
- [System Architecture Diagram](architecture/diagrams/system-architecture.mmd)
- [Module Dependencies](architecture/diagrams/module-dependencies.mmd)

"""
        
        # Try to find "Architecture" section
        if re.search(r'##\s+Architecture', content):
            # Replace or append to architecture section
            content = re.sub(
                r'(##\s+Architecture.*?\n\n)',
                r'\1' + arch_text,
                content,
                count=1
            )
        else:
            # Add before ## Module Structure or at end
            if "## Module Structure" in content:
                content = content.replace(
                    "## Module Structure",
                    "## Architecture\n\n" + arch_text + "\n## Module Structure"
                )
            else:
                content += "\n\n## Architecture\n\n" + arch_text
        
        with open(readme_path, 'w') as f:
            f.write(content)
        print("   ✓ README.md updated")
    
    def _generate_system_architecture_diagram(self) -> str:
        """Generate system architecture diagram."""
        lines = ["```mermaid", "graph TB"]
        lines.append("    classDef moduleStyle fill:#e1f5ff,stroke:#0288d1,stroke-width:2px")
        lines.append("    classDef rootStyle fill:#c8e6c9,stroke:#388e3c,stroke-width:3px")
        lines.append("")
        
        node_map = {}
        counter = [0]
        
        for module_path in self.parsed_tree['root_modules']:
            self._add_module_nodes(lines, module_path, node_map, counter)
        
        for module_path in self.parsed_tree['root_modules']:
            if module_path in node_map:
                lines.append(f"    class {node_map[module_path]} rootStyle")
        
        lines.append("```")
        return "\n".join(lines)
    
    def _add_module_nodes(self, lines: List[str], module_path: str,
                          node_map: Dict, counter: List[int], parent_id: str = None):
        """Recursively add module nodes."""
        node_id = f"M{counter[0]}"
        node_map[module_path] = node_id
        counter[0] += 1
        
        module_name = module_path.split('/')[-1]
        if len(module_name) > 15:
            module_name = module_name[:12] + "..."
        lines.append(f'    {node_id}["{module_name}"]')
        
        if parent_id:
            lines.append(f"    {parent_id} --> {node_id}")
        
        module_info = self.parsed_tree['modules'][module_path]
        for child_path in module_info['children'].keys():
            self._add_module_nodes(lines, child_path, node_map, counter, node_id)
    
    def _generate_module_dependency_diagram(self) -> str:
        """Generate module dependency diagram."""
        lines = ["```mermaid", "graph LR"]
        lines.append("    classDef highDep fill:#ffcdd2,stroke:#c62828")
        lines.append("    classDef medDep fill:#fff9c4,stroke:#f9a825")
        lines.append("    classDef lowDep fill:#c8e6c9,stroke:#388e3c")
        lines.append("")
        
        # Create simplified version with just module names
        node_map = {}
        for idx, module_path in enumerate(list(self.parsed_tree['modules'].keys())[:15]):
            node_id = f"M{idx}"
            node_map[module_path] = node_id
            module_name = module_path.split('/')[-1]
            if len(module_name) > 12:
                module_name = module_name[:9] + "..."
            lines.append(f'    {node_id}["{module_name}"]')
        
        lines.append("```")
        return "\n".join(lines)
    
    def _generate_component_overview_diagram(self) -> str:
        """Generate component overview diagram."""
        lines = ["```mermaid", "graph TB"]
        lines.append("    classDef classType fill:#bbdefb,stroke:#1976d2")
        lines.append("    classDef funcType fill:#c5e1a5,stroke:#689f38")
        lines.append("    classDef moduleType fill:#fff9c4,stroke:#f9a825")
        lines.append("")
        
        by_type = {}
        for comp_id, comp_data in list(self.dependency_graph.items())[:50]:
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
    
    def _generate_dataflow_diagram(self) -> str:
        """Generate data flow diagram."""
        lines = ["```mermaid", "graph LR"]
        lines.append("    classDef dataSource fill:#e3f2fd,stroke:#1976d2")
        lines.append("    classDef processor fill:#fff3e0,stroke:#f57c00")
        lines.append("    classDef dataStore fill:#f3e5f5,stroke:#7b1fa2")
        lines.append("")
        
        sources = []
        processors = []
        stores = []
        
        for comp_id, comp_data in list(self.dependency_graph.items())[:50]:
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
        
        lines.append("```")
        return "\n".join(lines)
    
    def _generate_architecture_overview_doc(self):
        """Generate architecture overview document."""
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
        
        arch_doc_path = self.arch_dir / "overview.md"
        with open(arch_doc_path, 'w') as f:
            f.write(md)
    
    def _inject_diagram_into_module_doc(self, module_path: str, doc_path: Path):
        """Inject diagram into existing module documentation."""
        with open(doc_path, 'r') as f:
            content = f.read()
        
        # Check if diagram already exists
        if "```mermaid" in content:
            return  # Already has diagrams
        
        # Find Architecture section
        arch_pattern = r'(## Architecture\s*\n)'
        match = re.search(arch_pattern, content)
        
        if match:
            # Generate component diagram
            diagram = self._generate_simple_component_diagram(module_path)
            
            # Insert diagram after Architecture heading
            insert_pos = match.end()
            new_content = (
                content[:insert_pos] +
                "\n### Component Diagram\n\n" +
                diagram + "\n\n" +
                content[insert_pos:]
            )
            
            with open(doc_path, 'w') as f:
                f.write(new_content)
    
    def _generate_simple_component_diagram(self, module_path: str) -> str:
        """Generate simple component diagram for a module."""
        lines = ["```mermaid", "graph LR"]
        lines.append("    classDef componentStyle fill:#e1f5ff,stroke:#0288d1,stroke-width:2px")
        lines.append("")
        
        # Add a placeholder node
        module_name = module_path.split('/')[-1]
        lines.append(f'    M0["{module_name}"]')
        lines.append("    class M0 componentStyle")
        
        lines.append("```")
        return "\n".join(lines)
    
    def _get_module_doc_path(self, module_path: str) -> Path:
        """Get path to module documentation."""
        path_parts = module_path.split('/')
        doc_path = self.docs_dir / "modules"
        
        for part in path_parts:
            doc_path = doc_path / part
        
        return doc_path / "README.md"
    
    def _save_diagram(self, filename: str, content: str):
        """Save diagram to file."""
        path = self.diagrams_dir / filename
        with open(path, 'w') as f:
            f.write(content)
    
    def _load_diagram(self, filename: str) -> str:
        """Load diagram from file."""
        path = self.diagrams_dir / filename
        try:
            with open(path, 'r') as f:
                return f.read()
        except:
            return f"<!-- Diagram {filename} not found -->"


def main():
    """Main entry point."""
    if len(sys.argv) < 4:
        print("Usage: python3 add_diagrams_to_docs.py <module_tree.json> <dependency_graph.json> <docs_dir>")
        print("\nExample:")
        print("  python3 add_diagrams_to_docs.py module_tree.json dependency_graph.json ./docs")
        sys.exit(1)
    
    module_tree_path = sys.argv[1]
    dependency_graph_path = sys.argv[2]
    docs_dir = sys.argv[3]
    
    print("=" * 60)
    print("CodeWiki Diagram Generator")
    print("Adds diagrams to existing documentation")
    print("=" * 60)
    
    generator = DiagramGenerator(module_tree_path, dependency_graph_path, docs_dir)
    
    # Generate all diagrams
    generator.generate_all_diagrams()
    
    # Inject into module docs
    generator.inject_diagrams_into_modules()
    
    # Update navigation
    generator.update_navigation()
    generator.update_readme()
    
    print("\n" + "=" * 60)
    print("✓ Done! Diagrams added to existing documentation")
    print("=" * 60)
    print(f"\nGenerated files:")
    print(f"  - {docs_dir}/architecture/overview.md")
    print(f"  - {docs_dir}/architecture/diagrams/*.mmd (4 files)")
    print(f"  - Updated existing module documentation")
    print(f"  - Updated INDEX.md and README.md")


if __name__ == '__main__':
    main()
