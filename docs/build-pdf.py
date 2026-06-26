#!/usr/bin/env python3
"""Genera PDF del documento de arquitectura con diagramas Mermaid como PNG."""

from __future__ import annotations

import base64
import re
import subprocess
import sys
import zlib
from pathlib import Path

import requests

DOCS_DIR = Path(__file__).resolve().parent
SOURCE_MD = DOCS_DIR / "ARQUITECTURA-ROADMAP-INTEGRACION.md"
DIAGRAMS_DIR = DOCS_DIR / "diagrams"
OUTPUT_MD = DOCS_DIR / "ARQUITECTURA-ROADMAP-INTEGRACION-pdf.md"
OUTPUT_HTML = DOCS_DIR / "ARQUITECTURA-ROADMAP-INTEGRACION.html"
OUTPUT_PDF = DOCS_DIR / "ARQUITECTURA-ROADMAP-INTEGRACION.pdf"

MERMAID_PATTERN = re.compile(r"```mermaid\s*\n(.*?)```", re.DOTALL)


def pako_deflate(data: str) -> str:
    """Codificación usada por mermaid.ink (deflate + base64url)."""
    compressed = zlib.compress(data.encode("utf-8"), level=9)[2:-4]
    encoded = base64.urlsafe_b64encode(compressed).decode("ascii")
    return encoded.rstrip("=")


def render_mermaid_png(diagram: str, output_path: Path, scale: int = 2) -> None:
    """Descarga PNG desde mermaid.ink; fallback a kroki.io."""
    diagram = diagram.strip()
    encoded = pako_deflate(diagram)

    urls = [
        f"https://mermaid.ink/img/pako:{encoded}?type=png&bgColor=white&width=1400&scale={scale}",
        "https://kroki.io/mermaid/png",
    ]

    last_error: Exception | None = None
    for i, url in enumerate(urls):
        try:
            if i == 0:
                resp = requests.get(url, timeout=120)
            else:
                resp = requests.post(
                    url,
                    data=diagram.encode("utf-8"),
                    headers={"Content-Type": "text/plain"},
                    timeout=120,
                )
            resp.raise_for_status()
            if len(resp.content) < 500:
                raise ValueError("Respuesta demasiado pequeña; posible error de renderizado")
            output_path.write_bytes(resp.content)
            return
        except Exception as exc:  # noqa: BLE001
            last_error = exc

    raise RuntimeError(f"No se pudo renderizar {output_path.name}: {last_error}")


def slugify(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def extract_diagrams(markdown: str) -> list[tuple[str, str]]:
    """Devuelve lista (id, contenido mermaid)."""
    diagrams: list[tuple[str, str]] = []
    for idx, match in enumerate(MERMAID_PATTERN.finditer(markdown), start=1):
        content = match.group(1).strip()
        first_line = content.split("\n", 1)[0][:40]
        diagram_id = f"{idx:02d}-{slugify(first_line) or 'diagrama'}"
        diagrams.append((diagram_id, content))
    return diagrams


def replace_mermaid_with_images(markdown: str, diagrams: list[tuple[str, str]]) -> str:
    result = markdown
    for diagram_id, content in diagrams:
        block = f"```mermaid\n{content}\n```"
        image_md = (
            f"\n![Diagrama {diagram_id}](diagrams/{diagram_id}.png)\n"
            f"{{ width=100% }}\n"
        )
        result = result.replace(block, image_md, 1)
    return result


def build_html(md_path: Path) -> str:
    import markdown as md_lib

    css = """
    @page { size: A4; margin: 2cm 1.8cm; }
    body {
        font-family: 'Segoe UI', Arial, sans-serif;
        font-size: 10.5pt;
        line-height: 1.45;
        color: #1a1a1a;
        max-width: 100%;
    }
    h1 {
        color: #1b5e20;
        font-size: 22pt;
        border-bottom: 2px solid #1b5e20;
        padding-bottom: 0.3em;
        page-break-after: avoid;
    }
    h2 {
        color: #2e7d32;
        font-size: 16pt;
        margin-top: 1.4em;
        page-break-after: avoid;
    }
    h3 {
        color: #388e3c;
        font-size: 12.5pt;
        page-break-after: avoid;
    }
    h4 { font-size: 11pt; page-break-after: avoid; }
    table {
        width: 100%;
        border-collapse: collapse;
        margin: 1em 0;
        font-size: 9.5pt;
        page-break-inside: avoid;
    }
    th, td {
        border: 1px solid #c8e6c9;
        padding: 6px 8px;
        text-align: left;
        vertical-align: top;
    }
    th { background: #e8f5e9; color: #1b5e20; }
    tr:nth-child(even) td { background: #f9fdf9; }
    img {
        display: block;
        max-width: 100%;
        height: auto;
        margin: 1em auto;
        page-break-inside: avoid;
    }
    code {
        background: #f1f8e9;
        padding: 1px 4px;
        border-radius: 3px;
        font-size: 9pt;
    }
    blockquote {
        border-left: 4px solid #81c784;
        margin: 1em 0;
        padding: 0.5em 1em;
        background: #f1f8e9;
        color: #33691e;
    }
    hr { border: none; border-top: 1px solid #c8e6c9; margin: 2em 0; }
    a { color: #2e7d32; text-decoration: none; }
    pre {
        background: #f5f5f5;
        padding: 0.8em;
        border-radius: 4px;
        font-size: 9pt;
        overflow-x: auto;
    }
    """

    text = md_path.read_text(encoding="utf-8")
    text = re.sub(r"\{ width=100% \}", "", text)
    html_body = md_lib.markdown(text, extensions=["tables", "fenced_code", "toc"])

    return f"""<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8"/>
  <title>Arquitectura, Roadmap e Integración</title>
  <style>{css}</style>
</head>
<body>
{html_body}
</body>
</html>"""


def find_browser() -> Path | None:
    candidates = [
        Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe"),
        Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"),
    ]
    for path in candidates:
        if path.exists():
            return path
    return None


def html_to_pdf_browser(html_path: Path, pdf_path: Path) -> None:
    browser = find_browser()
    if browser is None:
        raise RuntimeError("No se encontró Edge ni Chrome para generar PDF")

    if pdf_path.exists():
        pdf_path.unlink()

    file_url = html_path.resolve().as_uri()
    cmd = [
        str(browser),
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        f"--print-to-pdf={pdf_path.resolve()}",
        file_url,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if result.returncode != 0 or not pdf_path.exists():
        raise RuntimeError(
            f"Error al generar PDF con navegador: {result.stderr or result.stdout}"
        )


def html_to_pdf_xhtml2pdf(html: str, pdf_path: Path) -> None:
    from xhtml2pdf import pisa

    with pdf_path.open("wb") as pdf_file:
        status = pisa.CreatePDF(html, dest=pdf_file, encoding="utf-8")
    if status.err:
        raise RuntimeError("xhtml2pdf reportó errores al generar el PDF")


def markdown_to_pdf(md_path: Path, html_path: Path, pdf_path: Path) -> None:
    html = build_html(md_path)
    html_path.write_text(html, encoding="utf-8")
    print(f"HTML intermedio: {html_path.name}")

    try:
        html_to_pdf_browser(html_path, pdf_path)
        print("PDF generado con Edge/Chrome headless")
        return
    except Exception as browser_error:  # noqa: BLE001
        print(f"  Navegador no disponible ({browser_error}); usando xhtml2pdf...")

    html_to_pdf_xhtml2pdf(html, pdf_path)
    print("PDF generado con xhtml2pdf")


def main() -> int:
    if not SOURCE_MD.exists():
        print(f"No se encontró {SOURCE_MD}", file=sys.stderr)
        return 1

    print("Leyendo markdown...")
    markdown = SOURCE_MD.read_text(encoding="utf-8")
    diagrams = extract_diagrams(markdown)
    print(f"Diagramas Mermaid encontrados: {len(diagrams)}")

    DIAGRAMS_DIR.mkdir(parents=True, exist_ok=True)

    for diagram_id, content in diagrams:
        png_path = DIAGRAMS_DIR / f"{diagram_id}.png"
        print(f"  Renderizando {diagram_id}.png ...")
        render_mermaid_png(content, png_path)

    pdf_md = replace_mermaid_with_images(markdown, diagrams)
    OUTPUT_MD.write_text(pdf_md, encoding="utf-8")
    print(f"Markdown intermedio: {OUTPUT_MD.name}")

    print("Generando PDF...")
    markdown_to_pdf(OUTPUT_MD, OUTPUT_HTML, OUTPUT_PDF)
    print(f"PDF generado: {OUTPUT_PDF}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
