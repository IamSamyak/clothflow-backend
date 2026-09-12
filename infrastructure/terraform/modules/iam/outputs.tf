output "ecs_execution_role_arn" {
  description = "ARN of the ECS task execution role"
  value       = aws_iam_role.ecs_execution.arn
}

output "ecs_task_role_arn" {
  description = "ARN of the ECS application task role"
  value       = aws_iam_role.ecs_task.arn
}

output "user_service_task_role_arn" {
  description = "IAM task role ARN for User Service"
  value       = aws_iam_role.user_service_task.arn
}