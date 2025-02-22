package de.dafuqs.spectrum.events.listeners;

import de.dafuqs.spectrum.networking.SpectrumS2CPacketSender;
import de.dafuqs.spectrum.particle.effect.BlockPosEventTransfer;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;

public class BlockPosEventQueue extends EventQueue<BlockPosEventQueue.EventEntry> {
	
	public BlockPosEventQueue(PositionSource positionSource, int range, Callback listener) {
		super(positionSource, range, listener);
	}
	
	@Override
	public void acceptEvent(World world, BlockPos pos, GameEvent event, Entity entity, BlockPos sourcePos) {
		if (world instanceof ServerWorld) {
			EventEntry eventEntry = new EventEntry(event, new BlockPos(pos.getX(), pos.getY(), pos.getZ()), MathHelper.floor(Math.sqrt(pos.getSquaredDistance(sourcePos)))); // copy
			int delay = eventEntry.distance * 2;
			this.schedule(eventEntry, delay);
			SpectrumS2CPacketSender.sendBlockPosEventTransferPacket((ServerWorld) world, new BlockPosEventTransfer(pos, this.positionSource, delay));
		}
	}
	
	public static class EventEntry {
		public GameEvent gameEvent;
		public BlockPos eventSourceBlockPos;
		public int distance;
		
		public EventEntry(GameEvent gameEvent, BlockPos eventSourceBlockPos, int distance) {
			this.gameEvent = gameEvent;
			this.eventSourceBlockPos = eventSourceBlockPos;
			this.distance = distance;
		}
	}
	
}