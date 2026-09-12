resource "aws_elasticache_subnet_group" "this" {

  name = "${var.replication_group_id}-subnet-group"

  subnet_ids = var.subnet_ids

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "redis"
  }
}


resource "aws_elasticache_replication_group" "this" {

  replication_group_id = var.replication_group_id

  description = var.description

  engine = "redis"

  engine_version = var.engine_version

  node_type = var.node_type

  port = var.port

  num_cache_clusters = 1

  subnet_group_name = aws_elasticache_subnet_group.this.name

  security_group_ids = var.security_group_ids

  automatic_failover_enabled = false

  multi_az_enabled = false

  at_rest_encryption_enabled = true

  transit_encryption_enabled = false

  apply_immediately = true

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "user-service"
    Purpose     = "rate-limiting"
  }
}