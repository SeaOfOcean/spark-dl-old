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

import com.intel.analytics.bigdl.tensor.{Storage, Tensor}
import com.intel.analytics.bigdl.utils.T
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class FlattenTableSpec extends FlatSpec with BeforeAndAfter with Matchers {
  "An FlattenTable" should "generate correct output and grad" in {
    val layer = new FlattenTable[Double]()
    val input = T(Tensor[Double](
      Storage(Array(1.0, 2, 3))),
      T(
        Tensor[Double](Storage(Array(4.0, 3, 2, 1))),
        T(
          Tensor[Double](Storage(Array(3.0, 2, 1)))
        )
      )
    )
    val expectedOutput = T(Tensor[Double](
      Storage(Array(1.0, 2, 3))), Tensor[Double](Storage(Array(4.0, 3, 2, 1))),
      Tensor[Double](Storage(Array(3.0, 2, 1)))
    )


    val expectedGradInput = T(Tensor[Double](
      Storage(Array(1.1, 2.0, 3))),
      T(
        Tensor[Double](Storage(Array(4.1, 3, 2, 1))),
        T(
          Tensor[Double](Storage(Array(3.1, 2, 1)))
        )
      )
    )
    val gradOutput = T(Tensor[Double](
      Storage(Array(1.1, 2, 3))), Tensor[Double](Storage(Array(4.1, 3, 2, 1))),
      Tensor[Double](Storage(Array(3.1, 2, 1)))
    )

    val start = System.nanoTime()
    val output = layer.forward(input)
    val gradInput = layer.backward(input, gradOutput)
    val end = System.nanoTime()

    output should be (expectedOutput)
    gradInput should be (expectedGradInput)
  }
}

