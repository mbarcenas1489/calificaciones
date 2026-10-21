package com.example.calification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Component(
  @SerialName("Id") val id: Int,
  @SerialName("Name") val name: String,
)

@Serializable
data class ClassGroup(
  @SerialName("Id") val id: Int,
  @SerialName("Number") val number: Int,
  @SerialName("ComponentId") val componentId: Int,
)

@Serializable
data class Student(
  @SerialName("Id") val id: Int,
  @SerialName("Name") val name: String,
  @SerialName("GroupId") val groupId: Int,
)

@Serializable
data class Evaluation(
  @SerialName("Id") val id: Int,
  @SerialName("Name") val name: String,
  @SerialName("Value") val value: Int,
  @SerialName("Description") val description: String? = null,
  @SerialName("IdGroup") val groupId: Int? = null,
)

@Serializable
data class StudentEvaluation(
  @SerialName("Id") val id: Int,
  @SerialName("IdStudent") val studentId: Int? = null,
  @SerialName("IdEvaluation") val evaluationId: Int? = null,
  @SerialName("Grade") val grade: Double? = null,
)
