output "file_system_id" {
  description = "EFS filesystem ID"
  value       = aws_efs_file_system.this.id
}

output "file_system_arn" {
  description = "EFS filesystem ARN"
  value       = aws_efs_file_system.this.arn
}