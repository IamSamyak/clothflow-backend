output "vpc_id" {
  description = "ID of the ClothFlow VPC"
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  value = [
    aws_subnet.public_a.id,
    aws_subnet.public_b.id
  ]
}

output "app_subnet_ids" {
  value = [
    aws_subnet.app_a.id,
    aws_subnet.app_b.id
  ]
}

output "db_subnet_ids" {
  value = [
    aws_subnet.db_a.id,
    aws_subnet.db_b.id
  ]
}

output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

output "app_security_group_id" {
  value = aws_security_group.app.id
}

output "db_security_group_id" {
  value = aws_security_group.db.id
}

output "redis_security_group_id" {

  description = "Security group ID for Redis"

  value = aws_security_group.redis.id
}

output "observability_security_group_id" {
  description = "Security group ID for ClothFlow observability services"
  value       = aws_security_group.observability.id
}

output "efs_security_group_id" {
  description = "Security group ID for EFS"
  value       = aws_security_group.efs.id
}