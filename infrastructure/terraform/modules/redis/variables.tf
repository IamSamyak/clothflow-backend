variable "replication_group_id" {
  description = "Redis replication group identifier"
  type        = string
}

variable "description" {
  description = "Redis replication group description"
  type        = string
}

variable "node_type" {
  description = "Redis node type"
  type        = string
  default     = "cache.t3.micro"
}

variable "port" {
  description = "Redis port"
  type        = number
  default     = 6379
}

variable "subnet_ids" {
  description = "Subnets where Redis will be placed"
  type        = list(string)
}

variable "security_group_ids" {
  description = "Security groups attached to Redis"
  type        = list(string)
}

variable "engine_version" {
  description = "Redis/Valkey engine version"
  type        = string
  default     = "7.2"
}