/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/caliper.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.gatling.plugin.carbynestack.protocol

import com.softwaremill.quicklens.ModifyPimp

object CsProtocolBuilder {

  implicit def csProtocolBuilder2CsProtocol(builder: CsProtocolBuilder): CsProtocol = builder.build

  def apply(): CsProtocolBuilder = new CsProtocolBuilder(CsProtocol())
}

case class CsProtocolBuilder(protocol: CsProtocol) {

  def amphoraEndpoints(amphoraEndpointsValue: List[String]): CsProtocolBuilder =
    this.modify(_.protocol.amphoraEndpoints).setTo(amphoraEndpointsValue)

  def ephemeralEndpoints(ephemeralEndpointsValue: List[String]): CsProtocolBuilder =
    this.modify(_.protocol.ephemeralEndpoints).setTo(ephemeralEndpointsValue)

  def prime(primeValue: String): CsProtocolBuilder = this.modify(_.protocol.prime).setTo(primeValue)

  def r(rValue: String): CsProtocolBuilder = this.modify(_.protocol.r).setTo(rValue)

  def invR(invRValue: String): CsProtocolBuilder = this.modify(_.protocol.invR).setTo(invRValue)

  def program(programValue: String): CsProtocolBuilder = this.modify(_.protocol.program).setTo(programValue)

  def build: CsProtocol = protocol
}
