/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.settings;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The type Network configuration allows to configure network latency and bandwidth.
 */
public class NetworkConfiguration {
  /**
   * Regions where nodes can exist.
   */
  public static final List<String> REGION_LIST = new ArrayList<>(
      Arrays.asList("NORTH_AMERICA", "EUROPE", "SOUTH_AMERICA", "ASIA_SOUTH", "ASIA_PACIFIC", "JAPAN",
          "AUSTRALIA"
      ));

  /**
   * LATENCY[i][j] is average latency from REGION_LIST[i] to REGION_LIST[j]
   * Unit: millisecond, for year 2015
   */
  private static final long[][] LATENCY_2015 = {
      {36, 119, 255, 310, 154, 208},
      {119, 12, 221, 242, 266, 350},
      {255, 221, 137, 347, 256, 269},
      {310, 242, 347, 99, 172, 278},
      {154, 266, 256, 172, 9, 163},
      {208, 350, 269, 278, 163, 22}
  };

  /**
   * LATENCY[i][j] is average latency from REGION_LIST[i] to REGION_LIST[j]
   * Unit: millisecond, for year 2019
   */
  private static final long[][] LATENCY_2019 = {
      {32, 124, 184, 198, 151, 189},
      {124, 11, 227, 237, 252, 294},
      {184, 227, 88, 325, 301, 322},
      {198, 237, 325, 85, 58, 198},
      {151, 252, 301, 58, 12, 126},
      {189, 294, 322, 198, 126, 16}
  };

  /**
   * LATENCY[i][j] is average latency from REGION_LIST[i] to REGION_LIST[j]
   * Unit: millisecond, for year 2022
   */
  private static final long[][] LATENCY_2022 = {
    {13, 39, 44, 64, 78, 44, 68},
    {39, 6, 90, 72, 54, 85, 78},
    {44, 71, 17, 108, 115, 98, 112},
    {68, 72, 108, 13, 30, 13, 72},
    {68, 54, 115, 30, 20, 23, 47},
    {57, 85, 98, 27, 23, 10, 102},
    {74, 88, 112, 71, 47, 107, 17}
  };
  private static final long[][] LATENCY_FITTING_CBR_22 ={
    {24,70,79,115,140,79,122},
    {70,12,161,129,97,152,140},
    {79,128,30,195,207,176,201},
    {122,129,195,24,54,24,129},
    {122,97,207,54,36,42,85},
    {103,152,176,48,42,18,183},
    {134,158,201,128,85,192,30}
  };

  /**
   * List of latency assigned to each region. (unit: millisecond)
   */
  public static  long[][] LATENCY = LATENCY_FITTING_CBR_22;

  /**
   * List of download bandwidth assigned to each region, and last element is Inter-regional
   * bandwidth. (unit: bit per second) for year 2015
   */
  private static final long[] DOWNLOAD_BANDWIDTH_2015 = {
      25000000, 24000000, 6500000, 10000000,
      17500000, 14000000, 6 * 1000000
  };

  /**
   * List of download bandwidth assigned to each region, and last element is Inter-regional
   * bandwidth. (unit: bit per second) for year 2019
   */
  private static final long[] DOWNLOAD_BANDWIDTH_2019 = {
      52000000, 40000000, 18000000, 22800000,
      22800000, 29900000, 6 * 1000000
  };

  private static final long[] DOWNLOAD_BANDWIDTH_2022 = {
    120000000/8, 84000000/8, 48000000/8,
     84000000/8, 240000000/8, 120000000/8, 43200000/8, 24000000/8
  };

  /**
   * List of download bandwidth assigned to each region, and last element is Inter-regional
   * bandwidth. (unit: bit per second)
   */
  public static  long[] DOWNLOAD_BANDWIDTH = DOWNLOAD_BANDWIDTH_2022;

  /**
   * List of upload bandwidth assigned to each region. (unit: bit per second), and last element
   * is Inter-regional bandwidth for year 2015
   */
  private static final long[] UPLOAD_BANDWIDTH_2015 = {
      4700000, 8100000, 1800000, 5300000,
      3400000, 5200000, 6 * 1000000
  };

  /**
   * List of upload bandwidth assigned to each region. (unit: bit per second), and last element
   * is Inter-regional bandwidth for year 2019
   */
  private static final long[] UPLOAD_BANDWIDTH_2019 = {
      19200000, 20700000, 5800000, 15700000,
      10200000, 11300000, 6 * 1000000
  };

  private static final long[] UPLOAD_BANDWIDTH_2022 = {
    60000000/8, 42000000/8, 24000000/8, 42000000/8,
     120000000/8, 60000000/8, 21600000/8, 12000000/8
  };

  private static final long[] UPLOAD_FITTING_CBR_22 = {6875000,4812500,2750000,4812500,13750000,6875000,2475000,1375000};

  /**
   * List of upload bandwidth assigned to each region. (unit: bit per second), and last element
   * is Inter-regional bandwidth.
   */
  public static  long[] UPLOAD_BANDWIDTH = UPLOAD_FITTING_CBR_22;


  /**
   * Region distribution Bitcoin 2015.
   */
  private static final double[] REGION_DISTRIBUTION_BITCOIN_2015 = {
      0.3869, 0.5159, 0.0113,
      0.0574, 0.0119, 0.0166
  };

  /**
   * Region distribution Bitcoin 2019.
   */
  private static final double[] REGION_DISTRIBUTION_BITCOIN_2019 = {
      0.3316, 0.4998, 0.0090,
      0.1177, 0.0224, 0.0195
  };

  private static final double[] REGION_DISTRIBUTION_BITCOIN_2022 = {
    0.414, 0.457, 0.0090,
    0.072, 0.011, 0.02, 0.018
  };

  /**
   * Region distribution Litecoin.
   */
  private static final double[] REGION_DISTRIBUTION_LITECOIN = {
      0.3661, 0.4791, 0.0149, 0.1022, 0.0238, 0.0139
  };

  /**
   * Region distribution Dogecoin.
   */
  private static final double[] REGION_DISTRIBUTION_DOGECOIN = {
      0.3924, 0.4879, 0.0212, 0.0697, 0.0106, 0.0182
  };

  /**
   * The distribution of node's region. Each value means the rate of the number of nodes in the
   * corresponding region to the number of all nodes.
   */
  public static final double[] REGION_DISTRIBUTION = REGION_DISTRIBUTION_BITCOIN_2022;

  /**
   * The cumulative distribution of number of outbound links for Bitcoin 2015.
   */
  private static final double[] DEGREE_DISTRIBUTION_BITCOIN_2015 = {
      0.025, 0.050, 0.075, 0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.85, 0.90, 0.95, 0.97,
      0.97, 0.98, 0.99, 0.995, 1.0
  };

  /**
   * The cumulative distribution of number of outbound links for Litecoin.
   */
  private static final double[] DEGREE_DISTRIBUTION_LITECOIN = {
      0.01, 0.02, 0.04, 0.07, 0.09, 0.14, 0.20, 0.28, 0.39, 0.5, 0.6, 0.69, 0.76, 0.81, 0.85, 0.87,
      0.89, 0.92, 0.93, 1.0
  };

  /**
   * The cumulative distribution of number of outbound links for Dogecoin.
   */
  private static final double[] DEGREE_DISTRIBUTION_DOGECOIN = {
      0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 1.0, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00,
      1.00, 1.00, 1.00, 1.0
  };

  /**
   * The cumulative distribution of number of outbound links. Cf. Andrew Miller et al.,
   * "Discovering bitcoin's public topology and influential nodes", 2015.
   */
  public static final double[] DEGREE_DISTRIBUTION = DEGREE_DISTRIBUTION_BITCOIN_2015;

  /**
   * propability of a connection between a adversarial node and a regular node to be affected by M
   */
  public static double Q = 0.0;

  /**
   * probability of a node to be adverserial
   */
  public static double P = 0.0;

  /*
   * delay by which a block gets delayed in ms
   */
  public static long M = 0;

  /*
   * timeout after which a regular node will send out the next REC message for the same block
   */
  public static long T = 1200000;

  public static boolean USEMININGPOOLS = false;
  /*
   *number and distribution of the biggest Mining pool without the "unkown" Miners
   */
  public static int PoolCount_A = 22;
  public static Double[] PoolProportion_A = {23.002, 19.702, 15.072, 8.859, 8.687, 2.418, 1.946, 1.755, 1.207, 1.061, 1.046, 0.997, 0.641, 0.296, 0.146, 0.109, 0.071, 0.041, 0.022, 0.022, 0.004, 0.004};

  /*
   *number and distribution of the biggest Mining pool with the "unkown" Miners as one pool
   */
  public static int PoolCount_B = 23;
  public static Double[] PoolProportion_B = {23.002, 19.702, 15.072, 12.890, 8.859, 8.687, 2.418, 1.946, 1.755, 1.207, 1.061, 1.046, 0.997, 0.641, 0.296, 0.146, 0.109, 0.071, 0.041, 0.022, 0.022, 0.004, 0.004};
  public static long NetworkMiningPower = 10000000;

  public static int PoolCount;
  public static Double[] PoolProportion;

  public static boolean getUSEMINGPOOLS() {
    return USEMININGPOOLS;
  }

  public static void setUSEMININGPOOLS(boolean bool) {
    USEMININGPOOLS = bool;
  }

  public static int getPoolCount() {
    return PoolCount;
  }

  public static void setPoolCount(int count) {
    PoolCount = count;
  }

  public static Double[] getPoolProportion() {
    return PoolProportion;
  }

  public static void setPoolProportions(Double[] pools) {
    PoolProportion = pools;
  }

  public static long getNetworkMiningPower() {
    return NetworkMiningPower;
  }

  public static void setNetworkMiningPower(long mining) {
    NetworkMiningPower = mining;
  }
}
