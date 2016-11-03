package com.intel.analytics.sparkdl.pvanet

import breeze.linalg.DenseMatrix
import com.intel.analytics.sparkdl.tensor.{Storage, Tensor}
import org.scalatest.{FlatSpec, Matchers}


class AnchorSpec extends FlatSpec with Matchers {
  "mkanchors" should "work properly" in {
    val hs = Tensor(Storage(Array[Float](12, 16, 22)))
    val ws = Tensor(Storage(Array[Float](23, 16, 11)))
    val anchors = Anchor._mkanchors(ws, hs, 7.5f, 7.5f)
    anchors.storage().toArray should be(Array(-3.5, 2.0, 18.5, 13.0,
      0.0, 0.0, 15.0, 15.0,
      2.5, -3.0, 12.5, 18.0))
  }

  "_whctrs" should "work properly" in {
    Anchor.whctrs(Tensor(Storage(Array[Float](0, 0, 15, 15)))) should be(Array(16, 16, 7.5, 7.5))
    Anchor.whctrs(Tensor(Storage(Array[Float](-3.5.toFloat, 2.0f, 18.5f, 13f)))) should be(Array(23.0, 12.0, 7.5, 7.5))
  }

  "_ratio_enum" should "work properly" in {
    Anchor.ratioEnum(Tensor(Storage(Array(0f, 0, 15, 15))),
      Tensor(Storage(Array(0.5f, 1, 2)))).storage().toArray should be(
      Array(-3.5, 2.0, 18.5, 13.0, 0.0, 0.0, 15.0, 15.0, 2.5, -3.0, 12.5, 18.0))
  }

  "_scale_enum" should "work well" in {
    Anchor.scaleEnum(Tensor(Storage(Array(2.5f, -3.0f, 12.5f, 18.0f))), Tensor(Storage(Array[Float](8, 16, 32)))).storage().toArray should be(
      Array(-36.0, -80.0, 51.0, 95.0, -80.0, -168.0, 95.0, 183.0, -168.0, -344.0, 183.0, 359.0)
    )
  }

  "_generate anchors" should "work well" in {
    val act1 = Anchor.generateAnchors(16, Tensor(Storage(Array[Float](0.5f, 1, 2))),
      Tensor(Storage(Array[Float](8, 16, 32))))
    val expected1 = DenseMatrix((-84.0, -40.0, 99.0, 55.0),
      (-176.0, -88.0, 191.0, 103.0),
      (-360.0, -184.0, 375.0, 199.0),
      (-56.0, -56.0, 71.0, 71.0),
      (-120.0, -120.0, 135.0, 135.0),
      (-248.0, -248.0, 263.0, 263.0),
      (-36.0, -80.0, 51.0, 95.0),
      (-80.0, -168.0, 95.0, 183.0),
      (-168.0, -344.0, 183.0, 359.0))
    assert(act1 === expected1)
    val act2 = Anchor.generateAnchors(16, Tensor(Storage(Array[Float](0.5f, 1, 2, 4, 8))),
      Tensor(Storage(Array[Float](8, 16, 32, 64, 128))))
    val expected2 = DenseMatrix(
      (-84.0, -40.0, 99.0, 55.0),
      (-176.0, -88.0, 191.0, 103.0),
      (-360.0, -184.0, 375.0, 199.0),
      (-728.0, -376.0, 743.0, 391.0),
      (-1464.0, -760.0, 1479.0, 775.0),
      (-56.0, -56.0, 71.0, 71.0),
      (-120.0, -120.0, 135.0, 135.0),
      (-248.0, -248.0, 263.0, 263.0),
      (-504.0, -504.0, 519.0, 519.0),
      (-1016.0, -1016.0, 1031.0, 1031.0),
      (-36.0, -80.0, 51.0, 95.0),
      (-80.0, -168.0, 95.0, 183.0),
      (-168.0, -344.0, 183.0, 359.0),
      (-344.0, -696.0, 359.0, 711.0),
      (-696.0, -1400.0, 711.0, 1415.0),
      (-24.0, -120.0, 39.0, 135.0),
      (-56.0, -248.0, 71.0, 263.0),
      (-120.0, -504.0, 135.0, 519.0),
      (-248.0, -1016.0, 263.0, 1031.0),
      (-504.0, -2040.0, 519.0, 2055.0),
      (-16.0, -184.0, 31.0, 199.0),
      (-40.0, -376.0, 55.0, 391.0),
      (-88.0, -760.0, 103.0, 775.0),
      (-184.0, -1528.0, 199.0, 1543.0),
      (-376.0, -3064.0, 391.0, 3079.0)
    )
    assert(act2 === expected2)
  }
}
