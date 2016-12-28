/*
 * Licensed to Intel Corporation under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Intel Corporation licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.analytics.bigdl.nn

import org.scalatest.{FlatSpec, Matchers}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.T

class ParallelTableSpec extends FlatSpec with Matchers {
  "hashcode()" should "behave correctly" in {
    val log = new Log[Double]()
    val exp = new Exp[Double]()
    val m1 = new ParallelTable[Double]()
    m1.add(log)
    m1.add(log)
    val m2 = new ParallelTable[Double]()
    m2.add(log)
    m2.add(log)
    val m3 = new ParallelTable[Double]()
    m3.add(log)
    m3.add(exp)
    val m4 = new ParallelTable[Double]()
    m4.add(log)
    m4.add(log)
    val input1 = Tensor[Double](3, 3).randn()
    val input2 = Tensor[Double](3).randn()
    val input = T(1 -> input1, 2 -> input2)
    m4.forward(input)

    m1.hashCode() should equal(m2.hashCode())
    m1.hashCode() should not equal null
    m1.hashCode() should not equal log.hashCode()
    m1.hashCode() should not equal m3.hashCode()
    m1.hashCode() should not equal m4.hashCode()
  }

  "equals()" should "behave correctly" in {
    val log = new Log[Double]()
    val exp = new Exp[Double]()
    val m1 = new ParallelTable[Double]()
    m1.add(log)
    m1.add(log)
    val m2 = new ParallelTable[Double]()
    m2.add(log)
    m2.add(log)
    val m3 = new ParallelTable[Double]()
    m3.add(log)
    m3.add(exp)
    val m4 = new ParallelTable[Double]()
    m4.add(log)
    m4.add(log)
    val input1 = Tensor[Double](3, 3).randn()
    val input2 = Tensor[Double](3).randn()
    val input = T(1 -> input1, 2 -> input2)
    m4.forward(input)

    m1 should equal(m2)
    m1 should not equal null
    m1 should not equal log
    m1 should not equal m3
    m1 should not equal m4
  }

  "A ParallelTable" should "generate correct output" in {
    val input = T(
      Tensor[Float](10).randn(),
      Tensor[Float](10).randn())

    val gradOutput = T(
      Tensor[Float](3).randn(),
      Tensor[Float](3).randn())

    val linear1 = new Linear[Float](10, 3)
    val linear2 = new Linear[Float](10, 3)
    val expectedOutput = T(
      linear1.updateOutput(input(1)),
      linear2.updateOutput(input(2)))

    val module = new ParallelTable[Float]()
    module.add(linear1)
    module.add(linear2)
    val mapOutput = module.forward(input)
    mapOutput should equal (expectedOutput)

    val expectedGradInput = T(
      linear1.updateGradInput(input(1), gradOutput(1)),
      linear2.updateGradInput(input(2), gradOutput(2)))
    val mapGradInput = module.backward(input, gradOutput)

    mapGradInput should equal (expectedGradInput)
  }
}
