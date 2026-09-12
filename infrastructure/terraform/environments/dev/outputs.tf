output "vpc_id" {
  value = module.vpc.vpc_id
}

output "public_subnet_ids" {
  value = module.vpc.public_subnet_ids
}

output "app_subnet_ids" {
  value = module.vpc.app_subnet_ids
}

output "db_subnet_ids" {
  value = module.vpc.db_subnet_ids
}

output "alb_security_group_id" {
  value = module.vpc.alb_security_group_id
}

output "app_security_group_id" {
  value = module.vpc.app_security_group_id
}

output "db_security_group_id" {
  value = module.vpc.db_security_group_id
}

output "alb_dns_name" {
  value = module.alb.alb_dns_name
}

output "alb_id" {
  value = module.alb.alb_id
}

output "target_group_arn" {
  value = module.alb.target_group_arn
}

output "product_service_repository_url" {
  value = module.ecr.product_service_repository_url
}

output "db_instance_id" {
  description = "RDS instance identifier"
  value       = module.rds.db_instance_id
}

output "db_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = module.rds.db_endpoint
}

output "db_port" {
  description = "RDS PostgreSQL port"
  value       = module.rds.db_port
}

output "db_name" {
  description = "ClothFlow database name"
  value       = module.rds.db_name
}

output "db_username" {
  description = "RDS master username"
  value       = module.rds.db_username
}

output "kafka_bootstrap_servers" {
  value = module.kafka.bootstrap_brokers
}

output "grafana_target_group_arn" {
  description = "Grafana ALB target group ARN"
  value       = module.alb.grafana_target_group_arn
}