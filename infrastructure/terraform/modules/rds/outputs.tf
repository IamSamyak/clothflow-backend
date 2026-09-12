output "db_instance_id" {
  description = "RDS instance identifier"
  value       = aws_db_instance.this.id
}

output "db_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.this.address
}

output "db_port" {
  description = "RDS PostgreSQL port"
  value       = aws_db_instance.this.port
}

output "db_name" {
  description = "Database name"
  value       = aws_db_instance.this.db_name
}

output "db_username" {
  description = "Database master username"
  value       = aws_db_instance.this.username
}

output "master_user_secret_arn" {
  description = "ARN of the RDS managed master user secret"
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}
