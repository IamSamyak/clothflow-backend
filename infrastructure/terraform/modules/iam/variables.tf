variable "rds_secret_arn" {
  description = "ARN of the RDS master user secret"
  type        = string
}

variable "shipping_webhook_secret_arn" {
  description = "ARN of the Shipping webhook secret"
  type        = string
}

variable "jwt_secret_arn" {
  description = "ARN of the User Service JWT key set secret"
  type        = string
}

variable "service_credentials_secret_arns" {
  description = "Secrets Manager ARNs containing service-to-service credentials"

  type = list(string)
}

variable "user_outbox_encryption_secret_arn" {
  description = "Secrets Manager ARN containing the User Service outbox encryption key"
  type        = string
}

variable "grafana_admin_secret_arn" {
  description = "ARN of the Grafana admin credentials secret"
  type        = string
}