package uart

import chiseltest._
import org.scalatest._
import chisel3._

class UartRxSpec extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Uart Rx(12 MHz, 115200 bps)"
  it should "recieve 0b01010101" in {
    test(new Rx(12, 115200)) { c =>
      // 104 clock
      c.io.rx.poke(true.B)
      c.clock.step(100)
      c.io.rx.poke(false.B) // start bit
      c.clock.step(104)
      c.io.rx.poke(1.B) // 1 bit 1
      c.clock.step(104)
      c.io.rx.poke(0.B) // 2 bit 0
      c.clock.step(104)
      c.io.rx.poke(1.B) // 3 bit 1
      c.clock.step(104)
      c.io.rx.poke(0.B) // 4 bit 0
      c.clock.step(104)
      c.io.rx.poke(1.B) // 5 bit 1
      c.clock.step(104)
      c.io.rx.poke(0.B) // 6 bit 0
      c.clock.step(104)
      c.io.rx.poke(1.B) // 7 bit 1
      c.clock.step(104)
      c.io.rx.poke(0.B) // 8 bit 0
      c.clock.step(104)
      c.io.rx.poke(true.B) // stop bit
      c.clock.step(104)
      c.io.dout.expect(0x55.asUInt()) // LSB
    }
  }

  behavior of "Uart Tx(12 MHz, 115200 bps)"
  it should "send 0b01010101" in {
    test(new Tx(12, 115200)) { c =>
      c.clock.setTimeout(0)
      // 104 clock
      c.io.din.poke("b01010101".U(8.W))
      c.io.cts.poke(true.B)
      c.io.run.poke(true.B) // send a data
      c.clock.step(1)
      c.io.run.poke(false.B)
      c.clock.step(52)
      c.io.tx.expect(false.B) // start bit
      c.clock.step(104)
      c.io.tx.expect(1.B) // 1 bit 1
      c.clock.step(104)
      c.io.tx.expect(0.B) // 2 bit 0
      c.clock.step(104)
      c.io.tx.expect(1.B) // 3 bit 1
      c.clock.step(104)
      c.io.tx.expect(0.B) // 4 bit 0
      c.clock.step(104)
      c.io.tx.expect(1.B) // 5 bit 1
      c.clock.step(104)
      c.io.tx.expect(0.B) // 6 bit 0
      c.clock.step(104)
      c.io.tx.expect(1.B) // 7 bit 1
      c.clock.step(104)
      c.io.tx.expect(0.B) // 8 bit 0
      c.clock.step(104)
      c.io.tx.expect(true.B) // stop bit
      c.clock.step(104)
    }
  }

  class UartLoopBack(freq: Int, baudRate: Int) extends Module {
    val io = IO(new Bundle {
      val din = Input(UInt(8.W))
      val run = Input(Bool())

      val rxBusy = Output(Bool())
      val txBusy = Output(Bool())
      val dout = Output(UInt(8.W))
    })

    val rx = Module(new Rx(freq, baudRate))
    val tx = Module(new Tx(freq, baudRate))

    // Host -->  Tx ---- serial data --> Rx --> 8 bit Host

    // Tx -> Rx
    rx.io.rx := tx.io.tx

    // Rx -> Tx
    tx.io.cts := rx.io.rts

    // Host -> Tx
    tx.io.din := io.din
    tx.io.run := io.run

    // Rx -> Host
    io.dout := rx.io.dout

    // busy flag
    io.txBusy := tx.io.busy
    io.rxBusy := rx.io.busy
  }

  behavior of "Uart Loop Back Test (12 MHz, 115200 bps)"
  it should "send one data and revieve same data" in {
    test(new UartLoopBack(12, 115200)) { c =>
      c.clock.setTimeout(0)

      // wait Tx ready
      while (c.io.txBusy.peek().litValue == 1) {
        c.clock.step(1)
      }

      // Send data via UART Tx
      c.io.din.poke("b01010101".U(8.W))
      c.io.run.poke(true.B) // send a data
      c.clock.step(1)
      c.io.run.poke(false.B)
      c.clock.step(10)

      // wait to recive data
      while (c.io.rxBusy.peek().litValue == 1) {
        c.clock.step(1)
      }

      c.io.dout.expect("b01010101".U(8.W))
    }
  }

  it should "send \"Hello Chisel World\" and revieve same data" in {
    test(new UartLoopBack(12, 115200)) { c =>
      c.clock.setTimeout(0)
      var rdata = Nil
      for (d <- "Hello Chisel World!!!") {
        // wait Tx ready
        while (c.io.txBusy.peek().litValue() == 1) {
          c.clock.step(1)
        }

        // Send data via UART Tx
        c.io.din.poke(d.toInt.U(8.W))
        c.io.run.poke(true.B) // send a data
        c.clock.step(1)
        c.io.run.poke(false.B)
        c.clock.step(10)

        // wait to recive data
        while (c.io.rxBusy.peek().litValue() == 1) {
          c.clock.step(1)
        }

        print(s"${c.io.dout.peek().litValue().toChar}")
        c.io.dout.expect(d.toInt.U(8.W))
      }
      println()
    }
  }
}
