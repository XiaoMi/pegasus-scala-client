package com.xiaomi.infra.pegasus.scalaclient

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import Serializers._

import scala.concurrent.duration.Duration

/**
  * [Copyright]
  * Author: oujinliang
  * 3/27/18 8:09 PM
  */

class TableSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
    val table = "scala_test"

    "client basic get/set/del" should "work" in {
        withClient { c =>
            val hashKey = 12345L
            delHashKey(c, table, hashKey)

            c.set(table, hashKey, "sort_1", "value_1")

            c.exists(table, hashKey, "sort_1") should equal(true)
            c.exists(table, hashKey, "sort_2") should equal(false)
            c.sortKeyCount(table, hashKey) should equal(1)

            c.get(table, hashKey, "sort_1").as[String] should equal("value_1")
            c.get(table, hashKey, "sort_2").asOpt[String] should equal(None)

            c.del(table, hashKey, "sort_1")
            c.exists(table, hashKey, "sort_1") should equal(false)
            c.sortKeyCount(table, hashKey) should equal(0)
            c.get(table, hashKey, "sort_1").asOpt[String] should equal(None)
        }
    }

    "client multi set/get/del" should "work" in {
        withClient { c =>
            val hashKey = 12345L

            val values = Seq("sort_1" -> "value_1", "sort_2" -> "value_2", "sort_3" -> "value_3")
            val sortKeys = values.unzip._1

            delHashKey(c, table, hashKey)

            c.multiSet(table, hashKey, values)

            sortKeys.foreach { k => c.exists(table, hashKey, k) should equal(true) }

            c.sortKeyCount(table, hashKey) should equal(sortKeys.size)

            val multigetValues = c.multiGet(table, hashKey, sortKeys).as[String].values
            multigetValues.size should equal(sortKeys.size)
            (0 until sortKeys.size).foreach { i => multigetValues(i) should equal(values(i)) }

            val multiGetWithLimit = c.multiGet(table, hashKey, sortKeys, 1, 0).as[String].values
            multiGetWithLimit.size should equal(1)

            val multiGetWithNil = c.multiGet(table, hashKey, Seq[String]())
            multiGetWithNil.values.size should equal(sortKeys.size)

            val multiGetSortKeys = c.multiGetSortKeys(table, hashKey)
            multiGetSortKeys.as[String].values.toSet should equal(sortKeys.toSet)

            c.multiDel(table, hashKey, Seq("sort_1", "sort_2"))
            c.sortKeyCount(table, hashKey) should equal(1)

            c.multiDel(table, hashKey, Seq("sort_3"))
            c.sortKeyCount(table, hashKey) should equal(0)

            val multigetValues2 = c.multiGet(table, hashKey, sortKeys).as[String].values
            multigetValues2.size should equal(0)
        }
    }

    "client batch get/set/del" should "work" in {
        withClient { c =>
            val hashKey = "batch"

            //test batchSet
            val batchSet1 = new SetItem[String, String, String]("batch_1", "sort_1", "value_1", Duration.Zero)
            val batchSet2 = new SetItem[String, String, String]("batch_2", "sort_2", "value_2", Duration.Zero)
            c.batchSet(table, Seq(batchSet1, batchSet2))
            c.exists(table, "batch_1", "sort_1") should equal(true)
            c.exists(table, "batch_2", "sort_2") should equal(true)

            //test batchSet2
            val batchSet3 = new SetItem[String, String, String]("batch_3", "sort_3", "value_3", Duration.Zero)
            val batchSet4 = new SetItem[String, String, String]("batch_4", "sort_4", "value_4", Duration.Zero)
            c.batchSet2(table, Seq(batchSet3, batchSet4))
            c.exists(table, "batch_3", "sort_3") should equal(true)
            c.exists(table, "batch_4", "sort_4") should equal(true)

            ////test batchMultitSet
            val batchMultiSet1 = new HashKeyData[String, String, String]("batchMultiSet_1", List(("sort_1", "value_1"), ("sort_2", "value_2")))
            val batchMultiSet2 = new HashKeyData[String, String, String]("batchMultiSet_2", List(("sort_1", "value_1"), ("sort_2", "value_2")))
            c.batchMultitSet(table, Seq(batchMultiSet1, batchMultiSet2))
            c.exists(table, "batchMultiSet_1", "sort_1") should equal(true)
            c.exists(table, "batchMultiSet_1", "sort_2") should equal(true)
            c.exists(table, "batchMultiSet_2", "sort_1") should equal(true)
            c.exists(table, "batchMultiSet_2", "sort_2") should equal(true)

            //test batchMultitSet2
            val batchMultiSet3 = new HashKeyData[String, String, String]("batchMultiSet_3", List(("sort_1", "value_1"), ("sort_2", "value_2")))
            val batchMultiSet4 = new HashKeyData[String, String, String]("batchMultiSet_4", List(("sort_1", "value_1"), ("sort_2", "value_2")))
            c.batchMultitSet(table, Seq(batchMultiSet3, batchMultiSet4))
            c.exists(table, "batchMultiSet_3", "sort_1") should equal(true)
            c.exists(table, "batchMultiSet_3", "sort_2") should equal(true)
            c.exists(table, "batchMultiSet_4", "sort_1") should equal(true)
            c.exists(table, "batchMultiSet_4", "sort_2") should equal(true)

            //test batchGet
            val batchGet1 = new PegasusKey[String, String]("batch_1", "sort_1")
            val batchGet2 = new PegasusKey[String, String]("batch_2", "sort_2")
            val values1 = List("value_1", "value_2")
            val res1 = c.batchGet(table, List(batchGet1, batchGet2)).as[String]
            res1.indices.foreach { i => res1(i) should equal(values1(i)) }

            //test batchGet2
            val batchGet3 = new PegasusKey[String, String]("batch_3", "sort_3")
            val batchGet4 = new PegasusKey[String, String]("batch_4", "sort_4")
            val values2 = List("value_3", "value_4")
            val res2 = c.batchGet(table, List(batchGet3, batchGet4)).as[String]
            res2.indices.foreach { i => res2(i) should equal(values2(i)) }

            //test batchMultiGet
            val batchMultiGet1 = ("batchMultiSet_1", Seq("sort_1", "sort_2"))
            val batchMultiGet2 = ("batchMultiSet_2", Seq("sort_1", "sort_2"))
            val res3 = c.batchMultiGet(table, Seq(batchMultiGet1, batchMultiGet2))
            val hashKeys1 = List("batchMultiSet_1", "batchMultiSet_2")
            res3.indices.foreach { i => res3(i).hashKey should equal(hashKeys1(i)) }

            //test batchMultiGet2
            val batchMultiGet3 = ("batchMultiSet_3", Seq("sort_1", "sort_2"))
            val batchMultiGet4 = ("batchMultiSet_4", Seq("sort_1", "sort_2"))
            val res4 = c.batchMultiGet(table, Seq(batchMultiGet3, batchMultiGet4))
            val hashKeys2 = List("batchMultiSet_3", "batchMultiSet_4")
            res4.indices.foreach { i => res4(i).hashKey should equal(hashKeys2(i)) }

            //test batchDel
            val batchDel1 = new PegasusKey[String, String]("batch_1", "sort_1")
            val batchDel2 = new PegasusKey[String, String]("batch_2", "sort_2")
            c.batchDel(table, Seq(batchDel1, batchDel2))
            c.exists(table, "batch_1", "sort_1") should equal(false)
            c.exists(table, "batch_2", "sort_2") should equal(false)

            //test batchDel2
            val batchDel3 = new PegasusKey[String, String]("batch_3", "sort_3")
            val batchDel4 = new PegasusKey[String, String]("batch_4", "sort_4")
            c.batchDel(table, Seq(batchDel3, batchDel4))
            c.exists(table, "batch_3", "sort_3") should equal(false)
            c.exists(table, "batch_4", "sort_4") should equal(false)

            //test batchMultiDel
            val batchMultiDel1 = ("batchMultiSet_1", Seq("sort_1", "sort_2"))
            val batchMultiDel2 = ("batchMultiSet_2", Seq("sort_1", "sort_2"))
            c.batchMultiDel(table, Seq(batchMultiDel1, batchMultiDel2))
            c.exists(table, "batchMultiSet_1", "sort_1") should equal(false)
            c.exists(table, "batchMultiSet_1", "sort_2") should equal(false)
            c.exists(table, "batchMultiSet_2", "sort_1") should equal(false)
            c.exists(table, "batchMultiSet_2", "sort_2") should equal(false)

            //test batchMultiDel2
            val batchMultiDel3 = ("batchMultiSet_3", Seq("sort_1", "sort_2"))
            val batchMultiDel4 = ("batchMultiSet_4", Seq("sort_1", "sort_2"))
            c.batchMultiDel(table, Seq(batchMultiDel3, batchMultiDel4))
            c.exists(table, "batchMultiSet_3", "sort_1") should equal(false)
            c.exists(table, "batchMultiSet_3", "sort_2") should equal(false)
            c.exists(table, "batchMultiSet_4", "sort_1") should equal(false)
            c.exists(table, "batchMultiSet_4", "sort_2") should equal(false)
        }
    }


  private def delHashKey[A](c: ScalaPegasusClient, table: String, hashKey: A)(implicit ser: Serializer[A]) = {
        val keys = c.multiGetSortKeys(table, hashKey)
        if (keys.values.nonEmpty) {
            c.multiDel(table, hashKey, keys.values)
        }
    }

    private def withClient(f: ScalaPegasusClient => Unit) = {
        var client: ScalaPegasusClient = null
        try {
            client = ScalaPegasusClientFactory.createClient("resource:///pegasus.properties")
            f(client)
        } finally {
            println("closing client")
            client.close
        }
    }
}