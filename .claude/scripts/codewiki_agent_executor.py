#!/usr/bin/env python3
"""
CodeWiki Agent Executor
Script auxiliar para o CodeWiki Documentation Agent no Claude Code
"""

import os
import sys
import json
import subprocess
from pathlib import Path
from typing import Dict, List, Tuple, Optional


class CodeWikiAgentExecutor:
    """Executor autônomo para geração de documentação CodeWiki."""
    
    def __init__(self, repo_path: str = "."):
        self.repo_path = Path(repo_path).resolve()
        self.analysis_dir = None
        self.output_dir = Path("docs")
        self.verbose = True
        
    def log(self, message: str, emoji: str = "ℹ️"):
        """Log com emoji."""
        if self.verbose:
            print(f"{emoji} {message}")
    
    def execute_command(self, command: str, description: str = None) -> Tuple[int, str, str]:
        """Executa comando shell."""
        if description:
            self.log(description, "⚙️")
        
        result = subprocess.run(
            command,
            shell=True,
            capture_output=True,
            text=True,
            cwd=self.repo_path
        )
        
        return result.returncode, result.stdout, result.stderr
    
    def find_analysis_files(self) -> Optional[Tuple[Path, Path]]:
        """
        Procura por arquivos de análise existentes.
        
        Returns:
            Tuple com (module_tree_path, dependency_graph_path) ou None
        """
        self.log("Procurando arquivos de análise...", "🔍")
        
        search_paths = [
            "temp",      # CodeWiki default output location
            "docs",
            "wiki",
            ".codewiki",
            "."
        ]
        
        for path in search_paths:
            full_path = self.repo_path / path
            if not full_path.exists():
                continue
            
            # Find module_tree.json (with or without prefix)
            module_tree = None
            if (full_path / "module_tree.json").exists():
                module_tree = full_path / "module_tree.json"
            elif (full_path / "temp" / "module_tree.json").exists():
                # Check temp subdirectory
                module_tree = full_path / "temp" / "module_tree.json"
            else:
                # Try with project name prefix in temp/
                temp_dir = full_path / "temp"
                if temp_dir.exists():
                    prefixed_files = list(temp_dir.glob("*_module_tree.json"))
                    if prefixed_files:
                        module_tree = prefixed_files[0]
                
                # Try with project name prefix in current directory
                if not module_tree:
                    prefixed_files = list(full_path.glob("*_module_tree.json"))
                    if prefixed_files:
                        module_tree = prefixed_files[0]
            
            # Find dependency_graph.json (with or without prefix)
            dep_graph = None
            
            # Try standard name
            if (full_path / "dependency_graph.json").exists():
                dep_graph = full_path / "dependency_graph.json"
            else:
                # Try in temp/dependency_graphs subdirectory (CodeWiki default)
                dep_graphs_dir = full_path / "temp" / "dependency_graphs"
                if dep_graphs_dir.exists():
                    prefixed_files = list(dep_graphs_dir.glob("*_dependency_graph.json"))
                    if prefixed_files:
                        dep_graph = prefixed_files[0]
                
                # Try in dependency_graphs subdirectory with prefix
                if not dep_graph:
                    dep_graphs_dir = full_path / "dependency_graphs"
                    if dep_graphs_dir.exists():
                        prefixed_files = list(dep_graphs_dir.glob("*_dependency_graph.json"))
                        if prefixed_files:
                            dep_graph = prefixed_files[0]
                
                # Try with prefix in current directory
                if not dep_graph:
                    prefixed_files = list(full_path.glob("*_dependency_graph.json"))
                    if prefixed_files:
                        dep_graph = prefixed_files[0]
            
            # If both files found, return them
            if module_tree and dep_graph:
                self.log(f"✓ Arquivos encontrados em: {path}", "✅")
                self.log(f"  Module tree: {module_tree.name}", "📄")
                self.log(f"  Dependency graph: {dep_graph.name}", "📄")
                return (module_tree, dep_graph)
        
        self.log("✗ Arquivos de análise não encontrados", "❌")
        return None
    
    def check_codewiki_installation(self) -> Optional[str]:
        """Verifica instalação do CodeWiki."""
        self.log("Verificando CodeWiki...", "🔍")
        
        # Verificar comando global
        returncode, _, _ = self.execute_command("command -v codewiki", None)
        if returncode == 0:
            self.log("✓ CodeWiki comando global encontrado", "✅")
            return "codewiki"
        
        # Verificar script local
        local_script = self.repo_path / "codewiki.py"
        if local_script.exists():
            self.log("✓ CodeWiki local encontrado", "✅")
            return f"python3 {local_script}"
        
        # Verificar repositório clonado
        codewiki_repo = self.repo_path.parent / "CodeWiki" / "main.py"
        if codewiki_repo.exists():
            self.log("✓ CodeWiki reference encontrado", "✅")
            return f"python3 {codewiki_repo}"
        
        self.log("✗ CodeWiki não encontrado", "❌")
        return None
    
    def install_codewiki(self) -> str:
        """Instala CodeWiki."""
        self.log("Instalando CodeWiki...", "📦")
        
        # Criar diretório temporário
        temp_dir = self.repo_path / ".codewiki-temp"
        temp_dir.mkdir(exist_ok=True)
        
        # Clonar repositório
        clone_cmd = "git clone https://github.com/FSoft-AI4Code/CodeWiki.git .codewiki-temp/CodeWiki"
        returncode, stdout, stderr = self.execute_command(clone_cmd, "Clonando repositório...")
        
        if returncode != 0:
            self.log(f"Erro ao clonar: {stderr}", "❌")
            raise Exception("Falha ao instalar CodeWiki")
        
        # Instalar dependências
        req_file = temp_dir / "CodeWiki" / "requirements.txt"
        if req_file.exists():
            install_cmd = f"pip install -r {req_file}"
            self.execute_command(install_cmd, "Instalando dependências...")
        
        codewiki_cmd = f"python3 {temp_dir}/CodeWiki/main.py"
        self.log("✓ CodeWiki instalado com sucesso", "✅")
        
        return codewiki_cmd
    
    def run_analysis(self, codewiki_cmd: str) -> Tuple[Path, Path]:
        """Executa análise do código."""
        self.log("Executando análise do código...", "🔍")
        
        # Criar diretório de saída
        analysis_output = self.repo_path / ".codewiki"
        analysis_output.mkdir(exist_ok=True)
        
        # Executar análise
        analyze_cmd = f"{codewiki_cmd} analyze {self.repo_path} --output {analysis_output}"
        returncode, stdout, stderr = self.execute_command(
            analyze_cmd,
            "Analisando repositório (isso pode levar alguns minutos)..."
        )
        
        if returncode != 0:
            self.log(f"Erro na análise: {stderr}", "❌")
            raise Exception("Falha na análise do código")
        
        # Verificar arquivos gerados
        module_tree = analysis_output / "module_tree.json"
        dep_graph = analysis_output / "dependency_graph.json"
        
        if not (module_tree.exists() and dep_graph.exists()):
            self.log("Arquivos de análise não foram gerados", "❌")
            raise Exception("Análise não gerou arquivos esperados")
        
        self.log("✓ Análise concluída com sucesso", "✅")
        return (module_tree, dep_graph)
    
    def load_analysis_data(self, module_tree_path: Path, dep_graph_path: Path) -> Tuple[Dict, Dict]:
        """Carrega dados de análise."""
        self.log("Carregando dados de análise...", "📊")
        
        with open(module_tree_path, 'r', encoding='utf-8') as f:
            module_tree = json.load(f)
        
        with open(dep_graph_path, 'r', encoding='utf-8') as f:
            dependency_graph = json.load(f)
        
        # Estatísticas
        total_modules = self._count_modules(module_tree)
        total_components = len(dependency_graph)
        max_depth = self._get_max_depth(module_tree)
        
        self.log(f"""
📊 Análise do Repositório:
   • Módulos: {total_modules}
   • Componentes: {total_components}
   • Profundidade: {max_depth} níveis
        """, "📊")
        
        return module_tree, dependency_graph
    
    def _count_modules(self, tree: Dict, count: int = 0) -> int:
        """Conta módulos recursivamente."""
        for module_data in tree.values():
            count += 1
            if module_data.get('children'):
                count = self._count_modules(module_data['children'], count)
        return count
    
    def _get_max_depth(self, tree: Dict, current_depth: int = 0) -> int:
        """Calcula profundidade máxima."""
        max_d = current_depth
        for module_data in tree.values():
            if module_data.get('children'):
                child_depth = self._get_max_depth(module_data['children'], current_depth + 1)
                max_d = max(max_d, child_depth)
        return max_d
    
    def generate_documentation(self, module_tree: Dict, dependency_graph: Dict):
        """Gera documentação completa."""
        self.log("Iniciando geração de documentação...", "📝")
        
        # Importar orchestrator
        try:
            sys.path.insert(0, str(Path(__file__).parent))
            from orchestrator import CodeWikiOrchestrator
            
            # Salvar dados temporariamente
            temp_module_tree = self.repo_path / ".codewiki" / "module_tree.json"
            temp_dep_graph = self.repo_path / ".codewiki" / "dependency_graph.json"
            
            with open(temp_module_tree, 'w', encoding='utf-8') as f:
                json.dump(module_tree, f, indent=2, ensure_ascii=False)
            
            with open(temp_dep_graph, 'w', encoding='utf-8') as f:
                json.dump(dependency_graph, f, indent=2, ensure_ascii=False)
            
            # Executar orchestrator
            orchestrator = CodeWikiOrchestrator(
                str(temp_module_tree),
                str(temp_dep_graph),
                str(self.output_dir)
            )
            
            orchestrator.generate_all_documentation()
            
        except ImportError:
            # Fallback: usar linha de comando
            self.log("Usando método alternativo de geração...", "⚙️")
            
            cmd = f"python3 orchestrator.py {temp_module_tree} {temp_dep_graph} {self.output_dir}"
            returncode, stdout, stderr = self.execute_command(cmd)
            
            if returncode != 0:
                raise Exception(f"Falha na geração: {stderr}")
        
        self.log("✓ Documentação gerada com sucesso", "✅")
    
    def validate_documentation(self):
        """Valida documentação gerada."""
        self.log("Validando documentação...", "🔍")
        
        required_files = [
            self.output_dir / "README.md",
            self.output_dir / "INDEX.md"
        ]
        
        missing = []
        for file in required_files:
            if not file.exists():
                missing.append(str(file))
        
        if missing:
            self.log(f"Arquivos faltando: {', '.join(missing)}", "⚠️")
            return False
        
        self.log("✓ Documentação validada", "✅")
        return True
    
    def generate_report(self):
        """Gera relatório final."""
        self.log("\n" + "="*60, "")
        self.log("RELATÓRIO DE GERAÇÃO DE DOCUMENTAÇÃO", "📊")
        self.log("="*60, "")
        
        # Contar arquivos
        if self.output_dir.exists():
            md_files = list(self.output_dir.rglob("*.md"))
            total_size = sum(f.stat().st_size for f in md_files)
            
            self.log(f"""
📦 Resumo:
   • Arquivos Markdown: {len(md_files)}
   • Tamanho total: {total_size / 1024:.1f} KB
   • Localização: {self.output_dir}

📖 Começar por:
   • Visão geral: {self.output_dir / 'README.md'}
   • Navegação: {self.output_dir / 'INDEX.md'}

💡 Próximos passos:
   1. Revisar documentação gerada
   2. Personalizar conforme necessário
   3. Commitar para o repositório: git add docs/ && git commit -m "docs: Add documentation"
            """, "")
        
        self.log("="*60, "")
    
    def run(self):
        """Executa o processo completo."""
        try:
            self.log("🚀 CodeWiki Documentation Agent", "")
            self.log("="*60, "")
            
            # Fase 1: Verificação
            self.log("\n1️⃣ FASE 1: Verificação", "")
            analysis_files = self.find_analysis_files()
            
            # Fase 2: Análise (se necessário)
            if not analysis_files:
                self.log("\n2️⃣ FASE 2: Análise do Código", "")
                
                codewiki_cmd = self.check_codewiki_installation()
                
                if not codewiki_cmd:
                    codewiki_cmd = self.install_codewiki()
                
                analysis_files = self.run_analysis(codewiki_cmd)
            
            module_tree_path, dep_graph_path = analysis_files
            
            # Fase 3: Carregar dados
            self.log("\n3️⃣ FASE 3: Análise Estrutural", "")
            module_tree, dependency_graph = self.load_analysis_data(module_tree_path, dep_graph_path)
            
            # Fase 4: Gerar documentação
            self.log("\n4️⃣ FASE 4: Geração de Documentação", "")
            self.generate_documentation(module_tree, dependency_graph)
            
            # Fase 5: Validação
            self.log("\n5️⃣ FASE 5: Validação", "")
            if self.validate_documentation():
                self.generate_report()
                return 0
            else:
                self.log("Falha na validação", "❌")
                return 1
                
        except Exception as e:
            self.log(f"\n❌ ERRO: {str(e)}", "")
            self.log("\nPara diagnóstico, execute com --debug", "")
            return 1


def main():
    """Ponto de entrada principal."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="CodeWiki Documentation Agent - Geração autônoma de documentação"
    )
    parser.add_argument(
        "--repo",
        default=".",
        help="Caminho para o repositório (padrão: diretório atual)"
    )
    parser.add_argument(
        "--output",
        default="docs",
        help="Diretório de saída (padrão: ./docs)"
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Modo silencioso"
    )
    parser.add_argument(
        "--debug",
        action="store_true",
        help="Modo debug com saída detalhada"
    )
    
    args = parser.parse_args()
    
    executor = CodeWikiAgentExecutor(args.repo)
    executor.output_dir = Path(args.output)
    executor.verbose = not args.quiet
    
    if args.debug:
        import logging
        logging.basicConfig(level=logging.DEBUG)
    
    sys.exit(executor.run())


if __name__ == "__main__":
    main()
