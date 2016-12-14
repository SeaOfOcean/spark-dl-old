/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package com.intel.analytics.bigdl.pvanet.layers

import com.intel.analytics.bigdl.nn.Criterion
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Table

import scala.reflect.ClassTag

class SmoothL1Criterion2[T: ClassTag](@transient val sigma: Double, @transient val num: Int = 0)
  (implicit ev: TensorNumeric[T]) extends Criterion[Tensor[T], Table, T] {
  @transient var gradInput: Tensor[T] = _
  @transient var buffer: Tensor[T] = _
  // diff holds (input - gt) * w_in
  @transient var diff: Tensor[T] = _
  @transient val sigma2 = sigma * sigma
  @transient var hasWeights = true

  override def updateOutput(input: Tensor[T], target: Table): T = {
    // the target are composed of gt, inside_weight, outside_weight
    assert(target.length() >= 1)
    val bboxTarget = target(1).asInstanceOf[Tensor[T]]
    var insideW: Tensor[T] = null
    var outsideW: Tensor[T] = null
    if (target.length() == 1) {
      hasWeights = false
      require(input.nElement() == bboxTarget.nElement())
    } else {
      hasWeights = true
      insideW = target(2).asInstanceOf[Tensor[T]]
      outsideW = target(3).asInstanceOf[Tensor[T]]
      require(insideW.nElement() == outsideW.nElement() &&
        insideW.nElement() == bboxTarget.nElement(),
        "the length of bbox target, insideW, outsideW must be equal")
    }

    if (diff == null) {
      diff = Tensor[T]()
    }
    diff.resizeAs(input).copy(input)
    // input - gt
    diff.add(ev.fromType(-1), bboxTarget)
    if (hasWeights) {
      // apply "inside" weights, (input - gt) * w_in
      diff.cmul(insideW)
    }


    if (buffer == null) {
      buffer = Tensor[T]
    }
    // |input - gt| * w_in
    buffer.resizeAs(diff).copy(diff).abs()
    val data = buffer.storage().array()
    for (i <- data.indices) {
      // f(x) = 0.5 * (sigma * x)^2          if |x| < 1 / sigma / sigma
      //        |x| - 0.5 / sigma / sigma    otherwise
      if (ev.isGreater(ev.fromType(1.0 / sigma2), data(i))) {
        data(i) = ev.times(ev.fromType[Double](sigma2),
          ev.times(ev.fromType(0.5), ev.times(data(i), data(i))))
      }
      else {
        data(i) = ev.minus(data(i), ev.fromType[Double](0.5 / sigma2))
      }
    }
    if (hasWeights) {
      // apply "outside" weights,  w_out * SmoothL1(|input - gt| * w_in)
      buffer.cmul(outsideW)
    }
    if (num > 0) {
      ev.divide(buffer.sum(), ev.fromType(num))
    } else {
      ev.divide(buffer.sum(), ev.fromType(input.size(1)))
    }
  }

  override def updateGradInput(input: Tensor[T], target: Table): Tensor[T] = {
    assert(target.length() >= 1)

    val bboxTarget = target(1).asInstanceOf[Tensor[T]]
    var insideW: Tensor[T] = null
    var outsideW: Tensor[T] = null
    if (target.length() == 1) {
      hasWeights = false
      require(input.nElement() == bboxTarget.nElement())
    } else {
      hasWeights = true
      insideW = target(2).asInstanceOf[Tensor[T]]
      outsideW = target(3).asInstanceOf[Tensor[T]]
      require(insideW.nElement() == outsideW.nElement() &&
        insideW.nElement() == bboxTarget.nElement(),
        "the length of bbox target, insideW, outsideW must be equal")
    }
    if (gradInput == null) {
      gradInput = Tensor[T]()
    }
    val data = diff.storage().array()
    for (i <- data.indices) {
      // f'(x) = sigma * sigma * x         if |x| < 1 / sigma / sigma
      //       = sign(x)
      val x = data(i)
      if (ev.isGreater(ev.fromType[Double](1.0 / sigma2), ev.abs(x))) {
        data(i) = ev.times(ev.fromType[Double](sigma2), x)
      } else {
        // sign(x) == (0<x) - (x<0)
        if (ev.isGreater(data(i), ev.fromType(0))) {
          data(i) = ev.fromType(1)
        } else if (ev.isGreater(ev.fromType(0), data(i))) {
          data(i) = ev.fromType(-1)
        } else {
          data(i) = ev.fromType(0)
        }
      }
    }
    val sign = ev.fromType(1)
    val alpha = if (num > 0) {
      ev.divide(sign, ev.fromType(num))
    } else {
      ev.divide(sign, ev.fromType(input.size(1)))
    }

    gradInput.resizeAs(diff).copy(diff).mul(alpha)
    if (hasWeights) {
      // scale by inside weight
      gradInput.cmul(insideW)
      // scale by outside weight
      gradInput.cmul(outsideW)
    }
    gradInput
  }
}
