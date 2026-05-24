import http.server
import socketserver
import os

PORT = 8011

if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)

    if not os.path.exists("index.html"):
        print("Erro: O arquivo 'index.html' não foi encontrado na pasta do script.")
        exit(1)

    Handler = http.server.SimpleHTTPRequestHandler

    print(f"Iniciando o servidor frontend em: http://localhost:{PORT}")
    print(f"Servindo os arquivos de: {os.getcwd()}")
    
    socketserver.TCPServer.allow_reuse_address = True
    
    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServidor finalizado.")
