#!/bin/bash
# Instala Docker, Docker Compose plugin y Nginx en Ubuntu 22.04/24.04 (EC2)
set -euo pipefail

echo "==> Actualizando paquetes..."
sudo apt-get update -y
sudo apt-get upgrade -y

echo "==> Instalando dependencias..."
sudo apt-get install -y ca-certificates curl gnupg nginx certbot python3-certbot-nginx ufw git

echo "==> Instalando Docker..."
if ! command -v docker &>/dev/null; then
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  sudo chmod a+r /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt-get update -y
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo usermod -aG docker "$USER"
  echo "   Usuario $USER agregado al grupo docker (reconectar SSH para aplicar)."
fi

echo "==> Firewall UFW..."
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw --force enable

echo "==> Nginx habilitado..."
sudo systemctl enable nginx
sudo systemctl start nginx

echo ""
echo "Instalación base completada."
echo "Siguiente paso: configurar .env, docker compose y nginx (ver docs/DESPLIEGUE-AWS.md)"
