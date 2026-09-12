output "primary_endpoint_address" {
  description = "Redis primary endpoint address"

  value = aws_elasticache_replication_group.this.primary_endpoint_address
}


output "primary_endpoint_port" {
  description = "Redis primary endpoint port"

  value = aws_elasticache_replication_group.this.port
}


output "replication_group_id" {
  description = "Redis replication group ID"

  value = aws_elasticache_replication_group.this.id
}