variable "vpc_id" {
  description = "ID of the VPC where RDS will be deployed"
  type        = string
}

variable "db_subnet_ids" {
  description = "Private database subnet IDs"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group ID for the RDS instance"
  type        = string
}

variable "db_name" {
  description = "Initial database name"
  type        = string
  default     = "clothflow"
}

variable "db_username" {
  description = "Master username for PostgreSQL"
  type        = string
  default     = "clothflow_admin"
}