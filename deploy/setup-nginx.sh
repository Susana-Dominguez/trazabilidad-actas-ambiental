#!/bin/bash
# Configura Nginx con el dominio y opcionalmente obtiene certificado HTTPS
set -euo pipefail

if [ ! -f .env ]; then
  echo "Error: no existe .env. Copie deploy/env.example a .env y edítelo."
  exit 1
fi

# shellcheck disable=SC1091
source .env

if [ -z "${DOMAIN:-}" ]; then
  echo "Error: DOMAIN no está definido en .env"
  exit 1
fi

echo "==> Configurando Nginx para $DOMAIN ..."
sed "s/DOMAIN/$DOMAIN/g" deploy/nginx/actas.conf.template | sudo tee /etc/nginx/sites-available/actas > /dev/null
sudo ln -sf /etc/nginx/sites-available/actas /etc/nginx/sites-enabled/actas
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx

echo "==> Nginx listo en HTTP."
echo ""
read -r -p "¿Obtener certificado HTTPS con Certbot ahora? (s/n): " RESP
if [[ "$RESP" =~ ^[sS]$ ]]; then
  read -r -p "Correo para Let's Encrypt (avisos de renovación): " CERT_EMAIL
  sudo certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$CERT_EMAIL" --redirect
  echo "HTTPS configurado para https://$DOMAIN"
else
  echo "Ejecute manualmente: sudo certbot --nginx -d $DOMAIN"
fi
