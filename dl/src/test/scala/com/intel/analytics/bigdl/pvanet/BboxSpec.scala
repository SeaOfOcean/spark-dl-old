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

package com.intel.analytics.bigdl.pvanet

import breeze.linalg.{DenseMatrix, convert}
import breeze.numerics.abs
import com.intel.analytics.bigdl.pvanet.utils.Bbox
import org.scalatest.FlatSpec

class BboxSpec extends FlatSpec {
  behavior of "BboxSpec"

  it should "bboxVote" in {
    val v1 = DenseMatrix((124.67757, 198.44446, 499.0, 319.46954, 0.44293448),
      (342.84973, 159.25734, 499.0, 306.46826, 0.105595924),
      (71.944336, 42.300797, 435.08072, 323.51843, 0.095248096))
    val v2 = DenseMatrix((19.352219, 163.53407, 499.0, 318.2702, 0.1923532),
      (124.67757, 198.44446, 499.0, 319.46954, 0.44293448),
      (0.0, 191.88805, 499.0, 321.89288, 0.118584715),
      (0.0, 99.202065, 493.6775, 321.6583, 0.1829617),
      (0.0, 190.09538, 391.3026, 328.06378, 0.053920552),
      (239.84499, 103.70652, 499.0, 321.04962, 0.100431755),
      (100.49286, 91.12562, 499.0, 323.65686, 0.18068084),
      (178.0736, 175.94618, 499.0, 321.1881, 0.37770998),
      (0.0, 118.12887, 386.87604, 319.87268, 0.084006935),
      (175.99504, 35.884323, 498.94446, 318.77704, 0.07011555),
      (121.67645, 182.41452, 478.1596, 320.16412, 0.38305077),
      (0.0, 30.538345, 492.15747, 312.7992, 0.07436083),
      (77.29045, 125.990456, 478.4983, 320.66547, 0.10816171),
      (123.633224, 205.55194, 447.4248, 319.41675, 0.11607359),
      (178.96523, 206.04062, 499.0, 317.30673, 0.07201823),
      (155.32773, 207.17589, 499.0, 316.96683, 0.07939793),
      (287.53674, 118.663925, 499.0, 309.00146, 0.10261241),
      (57.928955, 130.33197, 408.22736, 317.33112, 0.25868088),
      (163.74406, 111.216034, 493.4549, 318.0517, 0.07715336),
      (92.417786, 190.0836, 426.7011, 319.14508, 0.053326167),
      (342.84973, 159.25734, 499.0, 306.46826, 0.105595924),
      (71.944336, 42.300797, 435.08072, 323.51843, 0.095248096))
    val out = Bbox.bboxVote(convert(v1, Float), convert(v2, Float))
    val expected = DenseMatrix((117.93, 181.086, 488.87, 319.773, 0.442934),
      (315.59, 139.251, 499.0, 307.717, 0.105596),
      (57.5732, 95.5176, 458.503, 320.046, 0.0952481))
    for (i <- 0 until out.rows) {
      for (j <- 0 until out.cols) {
        assert(Math.abs(out(i, j) - expected(i, j)) < 1e-3)
      }
    }
  }


  it should "bboxTransformInv" in {
    val boxes = DenseMatrix(
      (0.543404941791, 0.278369385094, 0.424517590749, 0.84477613232),
      (0.00471885619097, 0.121569120783, 0.670749084727, 0.825852755105),
      (0.136706589685, 0.575093329427, 0.891321954312, 0.209202122117),
      (0.18532821955, 0.108376890464, 0.219697492625, 0.978623784707),
      (0.811683149089, 0.171941012733, 0.816224748726, 0.274073747042))

    val deltas = DenseMatrix(
      (0.431704183663, 0.940029819622, 0.817649378777, 0.336111950121,
        0.175410453742, 0.37283204629, 0.00568850735257, 0.252426353445),
      (0.795662508473, 0.0152549712463, 0.598843376928, 0.603804539043,
        0.105147685412, 0.381943444943, 0.0364760565926, 0.890411563442),
      (0.980920857012, 0.059941988818, 0.890545944729, 0.5769014994,
        0.742479689098, 0.630183936475, 0.581842192399, 0.0204391320269),
      (0.210026577673, 0.544684878179, 0.769115171106, 0.250695229138,
        0.285895690407, 0.852395087841, 0.975006493607, 0.884853293491),
      (0.359507843937, 0.598858945876, 0.354795611657, 0.340190215371,
        0.178080989506, 0.237694208624, 0.0448622824608, 0.505431429636)
    )

    val expectedResults = DenseMatrix(
      (0.36640674522, 1.43795206519, 2.36227582099, 3.63013155633,
        0.69544806191, 0.637483321098, 1.58158720973, 2.6536754621),
      (0.64723382891, -0.558912463015, 3.67942969367, 2.55833193458,
        0.148952007861, -0.451279106784, 1.87687437777, 3.70058090758),
      (0.597628476893, 0.365638008002, 4.8726776815, 1.49467692786,
        0.746986104878, 0.968151508352, 3.88657497995, 1.61535429345),
      (-0.196252115502, 0.860638198136, 2.03576790462, 3.26375288055,
        -0.372917280555, 0.372232468489, 2.36938642765, 4.90314673809),
      (0.958912029942, 0.608662780166, 2.39127703713, 2.15739607458,
        0.967526811475, 0.0714749295084, 2.01816061047, 1.89848096643)
    )

    val res = Bbox.bboxTransformInv(convert(boxes, Float), convert(deltas, Float))

    assert(res.rows == expectedResults.rows && res.cols == expectedResults.cols)
    for (i <- 0 until res.rows) {
      for (j <- 0 until res.rows) {
        assert(abs(res(i, j) - expectedResults(i, j)) < 1e-6)
      }
    }
  }


  it should "clipBoxes" in {
    val boxes = DenseMatrix(
      (43.1704183663, 94.0029819622, 81.7649378777, 33.6111950121,
        17.5410453742, 37.283204629, 0.568850735257, 25.2426353445),
      (79.5662508473, 1.52549712463, 59.8843376928, 60.3804539043,
        10.5147685412, 38.1943444943, 3.64760565926, 89.0411563442),
      (98.0920857012, 5.9941988818, 89.0545944729, 57.69014994,
        74.2479689098, 63.0183936475, 58.1842192399, 2.04391320269),
      (21.0026577673, 54.4684878179, 76.9115171106, 25.0695229138,
        28.5895690407, 85.2395087841, 97.5006493607, 88.4853293491),
      (35.9507843937, 59.8858945876, 35.4795611657, 34.0190215371,
        17.8080989506, 23.7694208624, 4.48622824608, 50.5431429636))


    val expectedResults = DenseMatrix(
      (19.0, 9.0, 19.0, 9.0, 17.5410453742, 9.0, 0.568850735257, 9.0),
      (19.0, 1.52549712463, 19.0, 9.0, 10.5147685412, 9.0, 3.64760565926, 9.0),
      (19.0, 5.9941988818, 19.0, 9.0, 19.0, 9.0, 19.0, 2.04391320269),
      (19.0, 9.0, 19.0, 9.0, 19.0, 9.0, 19.0, 9.0),
      (19.0, 9.0, 19.0, 9.0, 17.8080989506, 9.0, 4.48622824608, 9.0)
    )

    val res = Bbox.clipBoxes(convert(boxes, Float), 10, 20)
    assert(res.rows == expectedResults.rows && res.cols == expectedResults.cols)
    for (i <- 0 until res.rows) {
      for (j <- 0 until res.cols) {
        assert(abs(res(i, j) - expectedResults(i, j)) < 1e-6)
      }
    }

    val boxes2 = TestUtil.loadDataFromFile(
      "/home/xianyan/code/intel/pvanet/transform_inv-300_84.txt", Array(300, 84))
    val expected = TestUtil.loadDataFromFile(
      "/home/xianyan/code/intel/pvanet/clip-300_84.txt", Array(300, 84)).toBreezeMatrix()
    val res2 = Bbox.clipBoxes(boxes2.toBreezeMatrix(), 1296, 2304)
    println(res2.rows, res2.cols, expected.rows, expected.cols)
    assert(res2.rows == expected.rows && res2.cols == expected.cols)
    for (i <- 0 until res2.rows) {
      for (j <- 0 until res2.cols) {
        assert(abs(res2(i, j) - expected(i, j)) < 1e-6)
      }
    }
  }

}
