output "cluster_arn" {
  description = "Kafka MSK cluster ARN"
  value       = aws_msk_cluster.this.arn
}

output "cluster_name" {
  description = "Kafka MSK cluster name"
  value       = aws_msk_cluster.this.cluster_name
}

output "bootstrap_brokers" {

  description = "Kafka bootstrap brokers"

  value = var.bootstrap_brokers_override != "" ? var.bootstrap_brokers_override : aws_msk_cluster.this.bootstrap_brokers

}
