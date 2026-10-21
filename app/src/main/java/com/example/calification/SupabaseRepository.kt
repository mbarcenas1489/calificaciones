package com.example.calification

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ComponentPayload(@SerialName("Name") val name: String)

@Serializable
private data class GroupPayload(
  @SerialName("Number") val number: Int,
  @SerialName("ComponentId") val componentId: Int,
)

@Serializable
private data class StudentPayload(
  @SerialName("Name") val name: String,
  @SerialName("GroupId") val groupId: Int,
)

@Serializable
private data class EvaluationPayload(
  @SerialName("Name") val name: String,
  @SerialName("Value") val value: Int,
  @SerialName("Description") val description: String?,
  @SerialName("IdGroup") val groupId: Int,
)

@Serializable
private data class StudentEvaluationPayload(
  @SerialName("IdStudent") val studentId: Int,
  @SerialName("IdEvaluation") val evaluationId: Int,
  @SerialName("Grade") val grade: Double,
)

class SupabaseRepository {
  private val client = createSupabaseClient(
    supabaseUrl = "https://zhbjkaqngrfutdqmxree.supabase.co",
    supabaseKey = "sb_publishable_b64PU0_aRnrw_VyfRSNRRg__NfInzen",
  ) {
    install(Postgrest)
  }

  suspend fun getComponents(): List<Component> =
    client.from("Component").select().decodeList<Component>().sortedBy { it.name.lowercase() }

  suspend fun getGroups(componentId: Int): List<ClassGroup> =
    client.from("Group").select {
      filter { eq("ComponentId", componentId) }
    }.decodeList<ClassGroup>().sortedBy { it.number }

  suspend fun getStudents(groupId: Int): List<Student> =
    client.from("Estudent").select {
      filter { eq("GroupId", groupId) }
    }.decodeList<Student>().sortedBy { it.name.lowercase() }

  suspend fun getEvaluations(groupId: Int): List<Evaluation> =
    client.from("Evaluations").select {
      filter { eq("IdGroup", groupId) }
    }.decodeList<Evaluation>().sortedBy { it.name.lowercase() }

  suspend fun getStudentEvaluations(studentId: Int): List<StudentEvaluation> =
    client.from("EstudentEvalucation").select {
      filter { eq("IdStudent", studentId) }
    }.decodeList<StudentEvaluation>()

  suspend fun getEvaluationGrades(evaluationId: Int): List<StudentEvaluation> =
    client.from("EstudentEvalucation").select {
      filter { eq("IdEvaluation", evaluationId) }
    }.decodeList<StudentEvaluation>()

  suspend fun createComponent(name: String) {
    client.from("Component").insert(ComponentPayload(name))
  }

  suspend fun updateComponent(id: Int, name: String) {
    client.from("Component").update(ComponentPayload(name)) {
      filter { eq("Id", id) }
    }
  }

  suspend fun deleteComponent(id: Int) {
    getGroups(id).forEach { deleteGroup(it.id) }
    client.from("Component").delete {
      filter { eq("Id", id) }
    }
  }

  suspend fun createGroup(componentId: Int, number: Int) {
    client.from("Group").insert(GroupPayload(number, componentId))
  }

  suspend fun updateGroup(id: Int, componentId: Int, number: Int) {
    client.from("Group").update(GroupPayload(number, componentId)) {
      filter { eq("Id", id) }
    }
  }

  suspend fun deleteGroup(id: Int) {
    getEvaluations(id).forEach { deleteEvaluation(it.id) }
    client.from("Estudent").delete {
      filter { eq("GroupId", id) }
    }
    client.from("Group").delete {
      filter { eq("Id", id) }
    }
  }

  suspend fun createStudent(groupId: Int, name: String) {
    client.from("Estudent").insert(StudentPayload(name, groupId))
  }

  suspend fun updateStudent(id: Int, groupId: Int, name: String) {
    client.from("Estudent").update(StudentPayload(name, groupId)) {
      filter { eq("Id", id) }
    }
  }

  suspend fun deleteStudent(id: Int) {
    client.from("EstudentEvalucation").delete {
      filter { eq("IdStudent", id) }
    }
    client.from("Estudent").delete {
      filter { eq("Id", id) }
    }
  }

  suspend fun createEvaluation(groupId: Int, name: String, value: Int, description: String?) {
    client.from("Evaluations").insert(EvaluationPayload(name, value, description, groupId))
  }

  suspend fun updateEvaluation(id: Int, groupId: Int, name: String, value: Int, description: String?) {
    client.from("Evaluations").update(EvaluationPayload(name, value, description, groupId)) {
      filter { eq("Id", id) }
    }
  }

  suspend fun deleteEvaluation(id: Int) {
    client.from("EstudentEvalucation").delete {
      filter { eq("IdEvaluation", id) }
    }
    client.from("Evaluations").delete {
      filter { eq("Id", id) }
    }
  }

  suspend fun saveGrades(
    evaluationId: Int,
    gradesByStudent: Map<Int, Double>,
    recordIdsToDelete: List<Int>,
  ) {
    if (gradesByStudent.isNotEmpty()) {
      val payloads = gradesByStudent.map { (studentId, grade) ->
        StudentEvaluationPayload(studentId, evaluationId, grade)
      }
      client.from("EstudentEvalucation").upsert(payloads) {
        onConflict = "IdStudent,IdEvaluation"
      }
    }
    if (recordIdsToDelete.isNotEmpty()) {
      client.from("EstudentEvalucation").delete {
        filter { isIn("Id", recordIdsToDelete) }
      }
    }
  }

}
