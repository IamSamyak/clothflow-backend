variable "vpc_id" {
  description = "VPC ID for Kafka"
  type        = string
}

variable "app_subnet_ids" {
  description = "Private application subnets for Kafka brokers"
  type        = list(string)
}

variable "app_security_group_id" {
  description = "ECS application security group"
  type        = string
}

variable "kafka_version" {
  description = "Kafka version for MSK"
  type        = string
  default     = "3.6.1"
}

variable "bootstrap_brokers_override" {
  description = "Optional Kafka bootstrap broker override for local environments"
  type        = string
  default     = ""
}
