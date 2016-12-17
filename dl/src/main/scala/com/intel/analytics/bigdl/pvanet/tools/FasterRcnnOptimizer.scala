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

package com.intel.analytics.bigdl.pvanet.tools

import com.intel.analytics.bigdl.Module
import com.intel.analytics.bigdl.dataset.LocalDataSet
import com.intel.analytics.bigdl.nn.ParallelCriterion
import com.intel.analytics.bigdl.optim.{OptimMethod, Trigger}
import com.intel.analytics.bigdl.pvanet.datasets.{ImageToTensor, ImageWithRoi, ObjectDataSource}
import com.intel.analytics.bigdl.pvanet.layers.AnchorTarget
import com.intel.analytics.bigdl.pvanet.model.FasterRcnn
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.Table

class FasterRcnnOptimizer(data: LocalDataSet[ImageWithRoi],
  validationData: ObjectDataSource,
  net: FasterRcnn,
  model: Module[Float],
  optimMethod: OptimMethod[Float],
  criterion: ParallelCriterion[Float],
  state: Table,
  endWhen: Trigger) {

  // todo: extends
  protected var cacheTrigger: Option[Trigger] = None
  protected var cachePath: Option[String] = None
  protected var isOverWrite: Boolean = false
  protected var validationTrigger: Option[Trigger] = None

  def setCache(path: String, trigger: Trigger): this.type = {
    this.cachePath = Some(path)
    this.cacheTrigger = Some(trigger)
    this
  }

  protected def saveModel(postfix: String = ""): this.type = {
    if (this.cachePath.isDefined) {
      model.save(s"${cachePath.get}.model$postfix", isOverWrite)
    }
    this
  }

  protected def saveState(state: Table, postfix: String = ""): this.type = {
    if (this.cachePath.isDefined) {
      state.save(s"${cachePath.get}.state$postfix", isOverWrite)
    }
    this
  }

  def setValidationTrigger(trigger: Trigger): this.type = {
    this.validationTrigger = Some(trigger)
    this
  }

  def overWriteCache(): this.type = {
    isOverWrite = true
    this
  }

  val anchorTarget = new AnchorTarget(net.param)

  /**
   *
   * @param d      image data class
   * @param output (rpn_cls, rpn_reg, cls, reg, rois)
   * @return
   */
  def generateTarget(d: ImageWithRoi, output: Table): Table = {
    val targets = new Table
    // anchor targets
    val sizes = output(2).asInstanceOf[Tensor[Float]].size()
    val height = sizes(sizes.length - 2)
    val width = sizes(sizes.length - 1)
    val anchorTargets = anchorTarget.getAnchorTarget(height, width,
      d.scaledImage.height(), d.scaledImage.width(), d.gtBoxes.get)
    targets.insert(anchorTargets.labels)
    targets.insert(anchorTargets.targetsTable)
//    val expected1 = BboxTarget(
//      FileUtil.loadFeatures[Float]("rpn_labels"),
//      FileUtil.loadFeatures[Float]("rpn_bbox_targets"),
//      FileUtil.loadFeatures[Float]("rpn_bbox_inside_weights"),
//      FileUtil.loadFeatures[Float]("rpn_bbox_outside_weights"))
    //require(anchorTargets.labels.equals(expected.labels))
//    targets.insert(expected1.labels)
//    targets.insert(expected1.targetsTable)


    // proposal targets
    val pTargets = output(5).asInstanceOf[Table]
//    val expected2 = BboxTarget(
//      FileUtil.loadFeatures[Float]("labels"),
//      FileUtil.loadFeatures[Float]("bbox_targets"),
//      FileUtil.loadFeatures[Float]("bbox_inside_weights"),
//      FileUtil.loadFeatures[Float]("bbox_outside_weights"))
//    targets.insert(expected2.labels)
//    targets.insert(expected2.targetsTable)
    targets.insert(pTargets(1))
    targets.insert(pTargets(2))
  }

  val imageToTensor = new ImageToTensor(batchSize = 1)

  def optimize(): Module[Float] = {
    val (weights, grad) = model.getParameters()
    var wallClockTime = 0L
    var count = 0

    state("epoch") = state.get[Int]("epoch").getOrElse(1)
    state("neval") = state.get[Int]("neval").getOrElse(1)
    var dataIter = data.data()
    data.shuffle()
    while (!endWhen(state)) {
      val start = System.nanoTime()
      val d = dataIter.next()
      val input = new Table
      input.insert(ImageToTensor(d))
      input.insert(d.imInfo.get)
      input.insert(d.gtBoxes.get)
      //      input.insert(FileUtil.loadFeatures[Float]("data"))
//      input.insert(FileUtil.loadFeatures[Float]("im_info").resize(3))
//      input.insert(FileUtil.loadFeatures[Float]("gt_boxes"))
val dataFetchTime = System.nanoTime()
      model.zeroGradParameters()
      // (rpn_cls, rpn_reg, cls, reg, proposalTargets)
      val output = model.forward(input)
      val target = generateTarget(d, output.asInstanceOf[Table])

      val loss = criterion.forward(output.asInstanceOf[Table], target)
      println(s"loss: $loss")
      println(s"  rpn cls loss: ${criterion.outputs(1)}")
      println(s"  rpn bbox loss: ${criterion.outputs(2)}")
      println(s"  cls loss: ${criterion.outputs(3)}")
      println(s"  bbox loss: ${criterion.outputs(4)}")
      val gradOutput = criterion.backward(output.asInstanceOf[Table], target)
      model.backward(input, gradOutput)
      optimMethod.optimize(_ => (loss, grad), weights, state)
      val end = System.nanoTime()
      wallClockTime += end - start
      count += 1
      println(s"[Epoch ${state[Int]("epoch")} $count/${data.size()}][Iteration ${
        state[Int]("neval")
      }][Wall Clock ${
        wallClockTime / 1e9
      }s] loss is $loss, iteration time is ${(end - start) / 1e9}s data " +
        s"fetch time is " +
        s"${(dataFetchTime - start) / 1e9}s, train time ${(end - dataFetchTime) / 1e9}s." +
        s" Throughput is ${1.toDouble / (end - start) * 1e9} img / second")
      state("neval") = state[Int]("neval") + 1

      if (count >= data.size()) {
        state("epoch") = state[Int]("epoch") + 1
        dataIter = data.data()
        data.shuffle()
        count = 0
      }
      validate(wallClockTime)
      cacheTrigger.foreach(trigger => {
        if (trigger(state) && cachePath.isDefined) {
          println(s"[Wall Clock ${wallClockTime / 1e9}s] Save model to ${cachePath.get}")
          saveModel(s".${state[Int]("neval")}")
          saveState(state, s".${state[Int]("neval")}")
        }
      })
    }
    validate(wallClockTime)
    model
  }

  private def validate(wallClockTime: Long): Unit = {
    validationTrigger.foreach(trigger => {
      if (trigger(state)) {
        println(s"[Wall Clock ${wallClockTime / 1e9}s] Validate model...")
        net.evaluate
        Test.testNet(net, validationData)
        net.train
      }
    })
  }
}

