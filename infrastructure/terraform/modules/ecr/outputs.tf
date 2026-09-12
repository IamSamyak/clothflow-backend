output "product_service_repository_url" {
  description = "ECR repository URL for Product Service"
  value       = aws_ecr_repository.product_service.repository_url
}

output "product_service_repository_arn" {
  description = "ECR repository ARN for Product Service"
  value       = aws_ecr_repository.product_service.arn
}

output "inventory_service_repository_url" {
  description = "ECR repository URL for Inventory Service"
  value       = aws_ecr_repository.inventory_service.repository_url
}

output "inventory_service_repository_arn" {
  description = "ECR repository ARN for Inventory Service"
  value       = aws_ecr_repository.inventory_service.arn
}

output "payment_service_repository_url" {
  description = "ECR repository URL for Payment Service"
  value       = aws_ecr_repository.payment_service.repository_url
}

# =========================================================
# Order Service
# =========================================================

output "order_service_repository_url" {
  description = "ECR repository URL for Order Service"
  value       = aws_ecr_repository.order_service.repository_url
}

output "order_service_repository_arn" {
  description = "ECR repository ARN for Order Service"
  value       = aws_ecr_repository.order_service.arn
}

# =========================================================
# Shipping Service
# =========================================================

output "shipping_service_repository_url" {
  description = "ECR repository URL for Shipping Service"
  value       = aws_ecr_repository.shipping_service.repository_url
}

output "shipping_service_repository_arn" {
  description = "ECR repository ARN for Shipping Service"
  value       = aws_ecr_repository.shipping_service.arn
}


# =========================================================
# Notification Service
# =========================================================

output "notification_service_repository_url" {
  description = "ECR repository URL for Notification Service"
  value       = aws_ecr_repository.notification_service.repository_url
}

output "notification_service_repository_arn" {
  description = "ECR repository ARN for Notification Service"
  value       = aws_ecr_repository.notification_service.arn
}

output "user_service_repository_url" {
  description = "ECR repository URL for User Service"
  value       = aws_ecr_repository.user_service.repository_url
}

output "prometheus_repository_url" {
  description = "ECR repository URL for Prometheus"
  value       = aws_ecr_repository.prometheus.repository_url
}

output "grafana_repository_url" {
  description = "ECR repository URL for Grafana"
  value       = aws_ecr_repository.grafana.repository_url
}