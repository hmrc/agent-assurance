/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//"InternalAuth" should {
//    "initialise the internal-auth token " in {
//
//
//      val expectedRequest = Json.obj(
//        "token" -> authToken,
//        "principal" -> appName,
//        "permissions" -> Seq(
//          Json.obj(
//            "resourceType" -> "dms-submission",
//            "resourceLocation" -> "submit",
//            "actions" -> List("WRITE")
//          )
//        )
//      )
//
//      getTestStubInternalAuthorised()
//      postTestStubInternalAuthorised()
//
//      GuiceApplicationBuilder()
//        .configure(
//          "internal-auth-token-enabled-on-start" -> true,
//          "internal-auth.token" -> authToken,
//          "microservice.services.internal-auth.host" -> wireMockHost,
//          "microservice.services.internal-auth.port" -> wireMockPort
//        ).build()
//
//      verifyGetTestStubInternalAuth(authToken)
//      verifyPostTestStubInternalAuth(expectedRequest)
//
//    }
//  }
//
//}*/
