FROM prom/prometheus:v3.5.0

# FLOCI-only:
# Docker SD accesses /var/run/docker.sock.
# FLOCI exposes the socket as root:1001 and its ECS emulator
# currently drops the ECS container "user" property.
# Therefore this development-only Prometheus image runs as root.
USER root

COPY prometheus.yml /etc/prometheus/prometheus.yml

EXPOSE 9090