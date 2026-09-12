variable "name" {
  description = "Name prefix for the EFS filesystem"
  type        = string
}

variable "subnet_ids" {
  description = "Subnets where EFS mount targets are created"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group attached to EFS mount targets"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}