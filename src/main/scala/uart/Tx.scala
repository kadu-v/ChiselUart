package uart

import chisel3._
import chisel3.util._
import scala.annotation.switch

class Tx(freq: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val din = Input(UInt(8.W))
    val run = Input(Bool())
    val cts = Input(Bool()) // H: active, L: inactive

    val tx = Output(Bool())
    val busy = Output(Bool())
  })

  val waitTime =
    ((freq * 1000000) / baudRate)
      .asUInt() // 50 MHz / 115200 = 50 * 10**6 / 115200

  val sIDLE :: sSTART :: sSEND :: sEND :: Nil = Enum(4)
  val txData = RegInit("b1111111111".U(10.W))
  val state = RegInit(sIDLE)
  val clkCnt = RegInit(0.U(15.W))
  val dataCnt = RegInit(0.U(4.W))
  val busy = RegInit(false.B)

  // connect register and output wire
  io.tx := txData(0)
  io.busy := busy | ~io.cts

  switch(state) {
    is(sIDLE) {
      when(io.run && io.cts) {
        state := sSTART
        busy := true.B
        txData := true.B ## io.din ## false.B // stop bit (1) | tx data (8) | start bit (1)
      }
    }
    is(sSTART) { // send a start bit
      when(clkCnt === waitTime - 1.asUInt()) {
        state := sSEND
        clkCnt := 0.asUInt()
        txData := true.B ## txData(9, 1) // LSB, shift
      }.otherwise {
        clkCnt := clkCnt + 1.asUInt()
      }
    }
    is(sSEND) { // send a tx data
      when(dataCnt === 8.asUInt()) {
        state := sEND
      }.elsewhen(clkCnt === waitTime - 1.asUInt()) {
        clkCnt := 0.asUInt()
        txData := true.B ## txData(9, 1) // LSB, shift
        dataCnt := dataCnt + 1.asUInt()
      }.otherwise {
        clkCnt := clkCnt + 1.asUInt()
      }
    }
    is(sEND) { // send a stop bit
      when(clkCnt === waitTime - 1.asUInt()) {
        state := sIDLE
        dataCnt := 0.asUInt()
        clkCnt := 0.asUInt()
        busy := false.B
      }.otherwise {
        clkCnt := clkCnt + 1.asUInt()
      }
    }
  }
}
