variable "execution_role_arn" {
  description = "ECS task execution role ARN"
  type        = string
}

variable "task_role_arn" {
  description = "ECS application task role ARN"
  type        = string
}

variable "shipping_webhook_secret_arn" {
  description = "ARN of the Shipping webhook secret"
  type        = string
}

# =========================================================
# Shared Network
# =========================================================

variable "app_subnet_ids" {
  description = "Private/app subnet IDs for ECS tasks"
  type        = list(string)
}

variable "app_security_group_id" {
  description = "Security group attached to ECS tasks"
  type        = string
}


# =========================================================
# Product Service
# =========================================================

variable "product_container_image" {
  description = "Container image for Product Service"
  type        = string
}

variable "product_log_group_name" {
  description = "CloudWatch log group for Product Service"
  type        = string
}

variable "product_target_group_arn" {
  description = "ALB target group ARN for Product Service"
  type        = string
}

variable "product_db_host" {
  description = "Product database host"
  type        = string
}

variable "product_db_port" {
  description = "Product database port"
  type        = string
}

variable "product_db_name" {
  description = "Product database name"
  type        = string
}

# =========================================================
# Inventory Service
# =========================================================

variable "inventory_container_image" {
  description = "Container image for Inventory Service"
  type        = string
}

variable "inventory_log_group_name" {
  description = "CloudWatch log group for Inventory Service"
  type        = string
}

variable "inventory_target_group_arn" {
  description = "ALB target group ARN for Inventory Service"
  type        = string
}

variable "inventory_db_host" {
  description = "Inventory database host"
  type        = string
}

variable "inventory_db_port" {
  description = "Inventory database port"
  type        = string
}

variable "inventory_db_name" {
  description = "Inventory database name"
  type        = string
}

variable "rds_secret_arn" {
  description = "ARN of the RDS managed master user secret"
  type        = string
}

# =========================================================
# Payment Service
# =========================================================

variable "payment_container_image" {
  description = "Container image for Payment Service"
  type        = string
}

variable "payment_log_group_name" {
  description = "CloudWatch log group for Payment Service"
  type        = string
}

variable "payment_target_group_arn" {
  description = "ALB target group ARN for Payment Service"
  type        = string
}

variable "payment_db_host" {
  description = "Payment database host"
  type        = string
}

variable "payment_db_port" {
  description = "Payment database port"
  type        = string
}

variable "payment_db_name" {
  description = "Payment database name"
  type        = string
}

# Order

variable "order_container_image" {
  description = "Container image for Order Service"
  type        = string
}

variable "order_log_group_name" {
  description = "CloudWatch log group for Order Service"
  type        = string
}

variable "order_target_group_arn" {
  description = "ALB target group ARN for Order Service"
  type        = string
}

variable "order_db_host" {
  description = "Order database host"
  type        = string
}

variable "order_db_port" {
  description = "Order database port"
  type        = string
}

variable "order_db_name" {
  description = "Order database name"
  type        = string
}

variable "shipping_container_image" {
  description = "Docker image for Shipping Service"
  type        = string
}

variable "shipping_log_group_name" {
  description = "CloudWatch log group for Shipping Service"
  type        = string
}

variable "shipping_target_group_arn" {
  description = "ALB target group ARN for Shipping Service"
  type        = string
}

variable "shipping_db_host" {
  description = "Shipping database host"
  type        = string
}

variable "shipping_db_port" {
  description = "Shipping database port"
  type        = string
}

variable "shipping_db_name" {
  description = "Shipping database name"
  type        = string
}



variable "notification_container_image" {
  description = "Docker image for Notification Service"
  type        = string
}

variable "notification_log_group_name" {
  description = "CloudWatch log group for Notification Service"
  type        = string
}

variable "notification_target_group_arn" {
  description = "ALB target group ARN for Notification Service"
  type        = string
}

variable "notification_db_host" {
  description = "Notification database host"
  type        = string
}

variable "notification_db_port" {
  description = "Notification database port"
  type        = string
}

variable "notification_db_name" {
  description = "Notification database name"
  type        = string
}

# =========================================================
# User Service
# =========================================================

variable "user_container_image" {
  description = "Container image for User Service"
  type        = string
}

variable "user_log_group_name" {
  description = "CloudWatch log group for User Service"
  type        = string
}

variable "user_target_group_arn" {
  description = "ALB target group ARN for User Service"
  type        = string
}

variable "user_db_host" {
  description = "User Service database host"
  type        = string
}

variable "user_db_port" {
  description = "User Service database port"
  type        = string
}

variable "user_db_name" {
  description = "User Service database name"
  type        = string
}

variable "user_task_role_arn" {
  description = "Dedicated IAM task role ARN for User Service"
  type        = string
}

variable "jwt_secret_arn" {
  description = "AWS Secrets Manager ARN containing the User Service JWT key set"
  type        = string
}

variable "user_outbox_encryption_secret_arn" {
  description = "ARN of the User Service outbox encryption secret"
  type        = string
}

variable "order_service_credentials_secret_arn" {
  description = "Secrets Manager ARN containing Order Service client credentials"

  type = string
}

variable "payment_service_credentials_secret_arn" {
  description = "Secrets Manager ARN containing Payment Service client credentials"

  type = string
}

variable "shipping_service_credentials_secret_arn" {
  description = "Secrets Manager ARN containing Shipping Service client credentials"

  type = string
}

# =========================================================
# Service Discovery
# =========================================================

# variable "service_discovery_namespace_id" {
#   description = "Cloud Map namespace ID for ECS service discovery"
#   type        = string
# }

# =========================================================
# Kafka
# =========================================================

variable "kafka_bootstrap_servers" {
  description = "Kafka bootstrap servers reachable by ECS tasks"
  type        = string
}


variable "user_redis_host" {

  description = "Redis endpoint for User Service"

  type = string
}


variable "user_redis_port" {

  description = "Redis port for User Service"

  type = number
}

variable "alb_dns_name" {
  description = "ALB DNS name used for internal service routing in the FLOCI dev environment"
  type        = string
}

variable "prometheus_container_image" {
  description = "Immutable Prometheus container image"
  type        = string
}

variable "observability_security_group_id" {
  description = "Security group used by observability services"
  type        = string
}

variable "prometheus_log_group_name" {
  description = "CloudWatch log group for Prometheus"
  type        = string
}


variable "grafana_container_image" {
  description = "Immutable Grafana container image"
  type        = string
}

variable "grafana_log_group_name" {
  description = "CloudWatch log group for Grafana"
  type        = string
}

variable "prometheus_service_host" {
  description = "Prometheus hostname reachable from Grafana"
  type        = string
}

variable "grafana_efs_file_system_id" {
  description = "EFS filesystem ID used for Grafana persistence"
  type        = string
}

variable "grafana_admin_secret_arn" {
  description = "ARN of the Grafana admin credentials secret"
  type        = string
}

variable "grafana_target_group_arn" {
  description = "ALB target group ARN for Grafana"
  type        = string
}

variable "grafana_root_url" {
  description = "External URL used by Grafana"
  type        = string
}