package com.intel.analytics.sparkdl.pvanet.model

import com.intel.analytics.sparkdl.nn._
import com.intel.analytics.sparkdl.tensor.Tensor
import com.intel.analytics.sparkdl.tensor.TensorNumericMath.TensorNumeric

import scala.reflect.ClassTag

object Pvanet {
  def getModel[T: ClassTag](implicit ev: TensorNumeric[T]): Module[Tensor[T], Tensor[T], T] = {
    val pvanet = new Sequential[Tensor[T], Tensor[T], T]()

    def convBNReLU(nInputPlane: Int,
                   nOutPutPlane: Int,
                   kernelW: Int,
                   kernelH: Int,
                   strideW: Int,
                   strideH: Int,
                   padW: Int,
                   padH: Int):
    Sequential[Tensor[T], Tensor[T], T] = {
      pvanet.add(new SpatialConvolution[T](nInputPlane, nOutPutPlane, kernelW, kernelH, strideW, strideH, padW, padH))
      pvanet.add(new SpatialBatchNormalization[T](nOutPutPlane))
      pvanet.add(new ReLU[T](true))
      pvanet
    }

    pvanet.add(new SpatialConvolution[T](16, 16, 7, 7, 2, 2, 3, 3, initMethod = Xavier))
//    pvanet.add(new BatchNormalization[T]())
  }

}