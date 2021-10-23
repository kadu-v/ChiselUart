package uart

import chisel3._

class Top extends Module {
  val io = IO(new Bundle {
    val PIO0 = Output(Bool())
  })
  withReset(~reset.asBool) {
    val c = Module(new Rx(12, 115200))
    c.io.rx := 1.asUInt()
    io.PIO0 := c.io.dout(0)
  }
}

object Elaborate extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Top(), args)
}
