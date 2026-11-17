#!/usr/bin/env bash
# One-time Azure set-up for the Messaging Service (Story 12): keys, its own database and user, the container app,
# and the front-end's route to it. Prints no secrets. Run once:  bash scripts/setup-messaging-azure.sh
set -euo pipefail

RG=rg-carebridge
ENVIRONMENT=cae-carebridge
SERVER=psql-carebridge-claudia
HOST=$SERVER.postgres.database.azure.com
ADMIN=cbadmin
DOMAIN=whitewave-91f35cbb.swedencentral.azurecontainerapps.io
SECRETS=~/.carebridge

echo "1/4 Keys (kept in $SECRETS; back them up like the others)"
[ -s "$SECRETS/messaging-db-password" ] || { openssl rand -base64 24 | tr -d '/+=' > "$SECRETS/messaging-db-password"; }
[ -s "$SECRETS/messaging-data-key" ] || { openssl rand -base64 32 > "$SECRETS/messaging-data-key"; }
chmod 600 "$SECRETS/messaging-db-password" "$SECRETS/messaging-data-key"

echo "2/4 Database 'messaging' and its own user (the firewall opens for this laptop only, and closes again)"
IP=$(curl -s https://api.ipify.org)
close_firewall() { az postgres flexible-server firewall-rule delete -g $RG --server-name $SERVER --name messaging-setup --yes -o none 2>/dev/null || true; }
trap close_firewall EXIT
az postgres flexible-server firewall-rule create -g $RG --server-name $SERVER --name messaging-setup \
  --start-ip-address "$IP" --end-ip-address "$IP" -o none
export PGPASSWORD; PGPASSWORD=$(cat "$SECRETS/pg-admin-password")
psql() { docker run --rm -i -e PGPASSWORD postgres:17-alpine psql -v ON_ERROR_STOP=1 "host=$HOST dbname=$1 user=$ADMIN sslmode=require" "${@:2}"; }
psql postgres -q -v pw="$(cat "$SECRETS/messaging-db-password")" <<'SQL'
SELECT 'CREATE ROLE messaging LOGIN PASSWORD ' || quote_literal(:'pw') WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'messaging') \gexec
SELECT 'GRANT messaging TO ' || current_user \gexec
SELECT 'CREATE DATABASE messaging OWNER messaging' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'messaging') \gexec
SQL
# Azure gives the public schema to azure_pg_admin; the service's own user must own it to create its tables.
psql messaging -q -c 'ALTER SCHEMA public OWNER TO messaging'
close_firewall

echo "3/4 Container app 'messaging-service' (the pipeline deploys the real image after the merge)"
secret() { az containerapp secret list -g $RG -n "$1" --show-values --query "[?name=='$2'].value | [0]" -o tsv; }
if ! az containerapp show -g $RG -n messaging-service -o none 2>/dev/null; then
  az containerapp create -g $RG -n messaging-service --environment $ENVIRONMENT \
    --image mcr.microsoft.com/k8se/quickstart:latest --ingress external --target-port 8080 \
    --cpu 0.5 --memory 1.0Gi --min-replicas 0 --max-replicas 1 \
    --secrets "messaging-db-password=$(cat "$SECRETS/messaging-db-password")" \
              "messaging-data-key=$(cat "$SECRETS/messaging-data-key")" \
              "gateway-secret=$(cat "$SECRETS/gateway-secret")" \
              "mail-password=$(cat "$SECRETS/brevo-smtp-key")" \
              "gp-emails=$(secret intake-service gp-emails)" \
              "assistant-emails=$(secret intake-service assistant-emails)" \
    --env-vars "MESSAGING_DB_URL=jdbc:postgresql://$HOST:5432/messaging?sslmode=require" MESSAGING_DB_USER=messaging \
               MESSAGING_DB_PASSWORD=secretref:messaging-db-password MESSAGING_DATA_KEY=secretref:messaging-data-key \
               GATEWAY_SECRET=secretref:gateway-secret GP_EMAILS=secretref:gp-emails ASSISTANT_EMAILS=secretref:assistant-emails \
               GOOGLE_CLIENT_ID=393612150902-7hrg287o9jutuqhohfm0mlnpicruur60.apps.googleusercontent.com \
               "ACCOUNT_JWKS_URI=https://account-service.$DOMAIN/.well-known/jwks.json" \
               "INTAKE_API_URL=https://intake-service.$DOMAIN" "SITE_URL=https://frontend.$DOMAIN" \
               MAIL_HOST=smtp-relay.brevo.com MAIL_PORT=587 MAIL_USERNAME=c28dbb001@smtp-brevo.com \
               MAIL_PASSWORD=secretref:mail-password MAIL_AUTH=true MAIL_STARTTLS_REQUIRED=true \
               "MAIL_FROM=CareBridge <andaclaudia2914@gmail.com>" -o none
fi

echo "4/4 The front-end's route to the Messaging Service"
az containerapp update -g $RG -n frontend --set-env-vars "MESSAGING_API_URL=https://messaging-service.$DOMAIN" -o none

echo "Done. Database, user, app and route are in place; no secret was printed."
