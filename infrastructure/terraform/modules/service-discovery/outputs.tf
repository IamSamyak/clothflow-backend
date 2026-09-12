output "namespace_id" {
  description = "Cloud Map private DNS namespace ID"
  value       = aws_service_discovery_private_dns_namespace.clothflow.id
}

output "namespace_arn" {
  description = "Cloud Map private DNS namespace ARN"
  value       = aws_service_discovery_private_dns_namespace.clothflow.arn
}

output "namespace_name" {
  description = "Cloud Map private DNS namespace name"
  value       = aws_service_discovery_private_dns_namespace.clothflow.name
}