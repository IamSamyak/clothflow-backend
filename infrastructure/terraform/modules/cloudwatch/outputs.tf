output "product_service_log_group_name" {
  description = "CloudWatch log group for Product Service"
  value       = aws_cloudwatch_log_group.product_service.name
}

output "inventory_service_log_group_name" {
  description = "CloudWatch log group for Inventory Service"
  value       = aws_cloudwatch_log_group.inventory_service.name
}

output "payment_service_log_group_name" {
  description = "CloudWatch log group for Payment Service"

  value = aws_cloudwatch_log_group.payment_service.name
}

output "order_service_log_group_name" {
  description = "CloudWatch log group for Order Service"
  value       = aws_cloudwatch_log_group.order_service.name
}

# =========================================================
# Shipping Service
# =========================================================

output "shipping_service_log_group_name" {
  description = "CloudWatch log group for Shipping Service"
  value       = aws_cloudwatch_log_group.shipping_service.name
}


# =========================================================
# Notification Service
# =========================================================

output "notification_service_log_group_name" {
  description = "CloudWatch log group for Notification Service"
  value       = aws_cloudwatch_log_group.notification_service.name
}


output "user_service_log_group_name" {
  description = "CloudWatch log group for User Service"
  value       = aws_cloudwatch_log_group.user_service.name
}

output "prometheus_log_group_name" {
  description = "CloudWatch log group for Prometheus"
  value       = aws_cloudwatch_log_group.prometheus.name
}

output "grafana_log_group_name" {
  description = "CloudWatch log group for Grafana"
  value       = aws_cloudwatch_log_group.grafana.name
}