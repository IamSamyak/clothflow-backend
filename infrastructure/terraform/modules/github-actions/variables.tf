variable "github_repository" {
  description = "GitHub repository in owner/repository format"
  type        = string
}

variable "github_branch" {
  description = "GitHub branch allowed to assume the deployment role"
  type        = string
  default     = "main"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "ecs_execution_role_arn" {
  description = "ARN of the ECS execution role that GitHub Actions may pass to ECS"
  type        = string
}

variable "ecs_task_role_arns" {
  description = "ARNs of ECS task roles that GitHub Actions may pass to ECS"
  type        = list(string)
  default     = []
}
