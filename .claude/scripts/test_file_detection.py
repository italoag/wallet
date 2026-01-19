#!/usr/bin/env python3
"""
Test script to demonstrate intelligent file detection
"""

from pathlib import Path
import json


def find_analysis_files(base_dir='.'):
    """
    Find module_tree and dependency_graph files with optional prefixes.
    Supports multiple naming conventions and directory structures.
    """
    search_paths = [
        ('temp', True),      # CodeWiki default output (with subdirectories)
        ('docs', True),      # Search with subdirectories
        ('wiki', False),     # No subdirectories
        ('.codewiki', False),
        ('.', False)
    ]
    
    for search_dir, check_subdirs in search_paths:
        path = Path(base_dir) / search_dir
        if not path.exists():
            continue
        
        print(f"🔍 Searching in: {path}")
        
        # Find module_tree
        module_tree_file = None
        if (path / 'module_tree.json').exists():
            module_tree_file = path / 'module_tree.json'
            print(f"  ✓ Found: {module_tree_file.name}")
        elif (path / 'temp' / 'module_tree.json').exists():
            module_tree_file = path / 'temp' / 'module_tree.json'
            print(f"  ✓ Found (in temp/): {module_tree_file.name}")
        else:
            # Try with project name prefix in temp/
            temp_dir = path / 'temp'
            if temp_dir.exists():
                matches = list(temp_dir.glob('*_module_tree.json'))
                if matches:
                    module_tree_file = matches[0]
                    print(f"  ✓ Found (with prefix in temp/): {module_tree_file.name}")
            
            # Try with project name prefix in current directory
            if not module_tree_file:
                matches = list(path.glob('*_module_tree.json'))
                if matches:
                    module_tree_file = matches[0]
                    print(f"  ✓ Found (with prefix): {module_tree_file.name}")
        
        # Find dependency_graph
        dep_graph_file = None
        
        # Check standard name first
        if (path / 'dependency_graph.json').exists():
            dep_graph_file = path / 'dependency_graph.json'
            print(f"  ✓ Found: {dep_graph_file.name}")
        else:
            # Check temp/dependency_graphs subdirectory (CodeWiki default)
            if check_subdirs:
                temp_dep_graphs_dir = path / 'temp' / 'dependency_graphs'
                if temp_dep_graphs_dir.exists():
                    print(f"  📁 Checking subdirectory: temp/dependency_graphs/")
                    matches = list(temp_dep_graphs_dir.glob('*_dependency_graph.json'))
                    if matches:
                        dep_graph_file = matches[0]
                        print(f"  ✓ Found (with prefix in temp/dependency_graphs/): {dep_graph_file.name}")
            
            # Check dependency_graphs subdirectory (common in CodeWiki)
            if not dep_graph_file and check_subdirs:
                dep_graphs_dir = path / 'dependency_graphs'
                if dep_graphs_dir.exists():
                    print(f"  📁 Checking subdirectory: {dep_graphs_dir.name}/")
                    matches = list(dep_graphs_dir.glob('*_dependency_graph.json'))
                    if matches:
                        dep_graph_file = matches[0]
                        print(f"  ✓ Found (with prefix in subdir): {dep_graph_file.name}")
            
            # Check current directory with prefix
            if not dep_graph_file:
                matches = list(path.glob('*_dependency_graph.json'))
                if matches:
                    dep_graph_file = matches[0]
                    print(f"  ✓ Found (with prefix): {dep_graph_file.name}")
        
        if module_tree_file and dep_graph_file:
            print(f"\n✅ SUCCESS! Found both files in: {search_dir}/")
            print(f"   Module tree:      {module_tree_file}")
            print(f"   Dependency graph: {dep_graph_file}")
            return module_tree_file, dep_graph_file
        elif module_tree_file or dep_graph_file:
            print(f"  ⚠️  Incomplete: Only found {'module_tree' if module_tree_file else 'dependency_graph'}")
    
    print("\n❌ Analysis files not found in any location")
    return None, None


def test_file_detection():
    """Test the file detection with examples."""
    
    print("="*70)
    print("CodeWiki File Detection Test")
    print("="*70)
    print("\nThis test demonstrates the intelligent file detection that supports:")
    print("  ✓ CodeWiki default output (temp/dependency_graphs/ProjectName_*.json)")
    print("  ✓ Standard names (dependency_graph.json)")
    print("  ✓ Project-prefixed names (ProjectName_dependency_graph.json)")
    print("  ✓ Files in subdirectories (docs/dependency_graphs/)")
    print("  ✓ Multiple search locations (temp/, docs/, wiki/, .codewiki/, ./)")
    print("\n" + "-"*70 + "\n")
    
    # Test detection
    module_tree_path, dep_graph_path = find_analysis_files()
    
    if module_tree_path and dep_graph_path:
        print("\n" + "="*70)
        print("📊 File Analysis")
        print("="*70)
        
        # Try to load and show basic info
        try:
            with open(module_tree_path, 'r') as f:
                module_tree = json.load(f)
            print(f"\n✓ Module tree loaded: {len(module_tree)} top-level modules")
            
            with open(dep_graph_path, 'r') as f:
                dependency_graph = json.load(f)
            print(f"✓ Dependency graph loaded: {len(dependency_graph)} components")
            
            print("\n🎉 Files are valid and ready for documentation generation!")
            
        except Exception as e:
            print(f"\n⚠️  Warning: Could not parse files: {e}")
    else:
        print("\n💡 No analysis files found. The agent would:")
        print("   1. Install CodeWiki automatically")
        print("   2. Run analysis on your repository")
        print("   3. Generate the documentation")
    
    print("\n" + "="*70)


if __name__ == "__main__":
    test_file_detection()
