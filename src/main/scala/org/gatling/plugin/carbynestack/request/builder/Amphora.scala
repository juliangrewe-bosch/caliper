/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/caliper.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.gatling.plugin.carbynestack.request.builder

import io.carbynestack.amphora.client.Secret
import io.gatling.commons.validation.{Failure, Success}
import io.gatling.core.session.Expression
import org.gatling.plugin.carbynestack.action.AmphoraActionBuilder
import org.gatling.plugin.carbynestack.request.client.AmphoraClientBuilder

class Amphora() {

  def createSecret(secret: Expression[Secret]): AmphoraActionBuilder =
    new AmphoraActionBuilder(
      new AmphoraClientBuilder,
      (client, session) => {
        val secretValue = secret(session) match {
          case Success(value)   => value
          case Failure(message) => throw new IllegalArgumentException(message)
        }
        client.createSecret(secretValue)
      }
    )

  /* def getSecrets(): BaseActionBuilder[AmphoraClient, java.util.List[Metadata]] =
    new BaseActionBuilder[AmphoraClient, java.util.List[Metadata]]()

  def getSecrets(filterCriteria: java.util.List[TagFilter]): BaseActionBuilder[AmphoraClient, java.util.List[Metadata]] =
    new BaseActionBuilder[AmphoraClient, java.util.List[Metadata]]()

  def getSecrets(sort: Sort): BaseActionBuilder[AmphoraClient, java.util.List[Metadata]] =
    new BaseActionBuilder[AmphoraClient, java.util.List[Metadata]]()*/
}
