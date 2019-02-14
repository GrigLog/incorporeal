package quaternary.incorporeal.cygnus.cap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import quaternary.incorporeal.Incorporeal;
import quaternary.incorporeal.api.cygnus.ICygnusFunnelable;
import quaternary.incorporeal.etc.helper.CorporeaHelper2;
import quaternary.incorporeal.item.cygnus.ItemCygnusTicket;
import vazkii.botania.api.corporea.CorporeaRequest;
import vazkii.botania.common.block.tile.corporea.TileCorporeaCrystalCube;
import vazkii.botania.common.block.tile.corporea.TileCorporeaRetainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class AttachCapabilitiesEventHandler {
	public static final ResourceLocation FUNNEL_HANDLER = new ResourceLocation(Incorporeal.MODID, "cygnus_funnel_handler");
	
	@SubscribeEvent
	public static void attachTileCapabilities(AttachCapabilitiesEvent<TileEntity> e) {
		TileEntity tile = e.getObject();
		
		if(tile instanceof TileCorporeaRetainer) {
			e.addCapability(FUNNEL_HANDLER, new GenericCapabilityProvider<>(
				IncorporeticCygnusCapabilities.FUNNEL_CAP,
				new CorporeaRetainerFunnelable((TileCorporeaRetainer) tile)
			));
		} else if(tile instanceof TileCorporeaCrystalCube) {
			e.addCapability(FUNNEL_HANDLER, new GenericCapabilityProvider<>(
				IncorporeticCygnusCapabilities.FUNNEL_CAP,
				new CorporeaCrystalCubeFunnelable((TileCorporeaCrystalCube) tile)
			));
		}
	}
	
	@SubscribeEvent
	public static void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> e) {
		Entity ent = e.getObject();
		
		if(ent instanceof EntityItem) {
			e.addCapability(FUNNEL_HANDLER, new GenericCapabilityProvider<>(
				IncorporeticCygnusCapabilities.FUNNEL_CAP,
				new ItemEntityFunnelable((EntityItem) ent)
			));
		} else if(ent instanceof EntityItemFrame) {
			e.addCapability(FUNNEL_HANDLER, new GenericCapabilityProvider<>(
				IncorporeticCygnusCapabilities.FUNNEL_CAP,
				new ItemFrameFunnelable((EntityItemFrame) ent)
			));
		}
	}
	
	//Just for saving typing ;)
	public static final class GenericCapabilityProvider<C> implements ICapabilityProvider {
		public GenericCapabilityProvider(Capability<C> cap, C impl) {
			this.cap = cap;
			this.impl = impl;
		}
		
		private final Capability<C> cap;
		private final C impl;
		
		@Override
		public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
			return capability == cap;
		}
		
		@Nullable
		@Override
		@SuppressWarnings("unchecked")
		public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
			return capability == cap ? (T) impl : null;
		}
	}
	
	//and now, the actual funnelables:
	private static final class CorporeaRetainerFunnelable implements ICygnusFunnelable {
		public CorporeaRetainerFunnelable(TileCorporeaRetainer retainer) {
			this.retainer = retainer;
		}
		
		private final TileCorporeaRetainer retainer;
		
		@Override
		public boolean canGiveCygnusItem() {
			return retainer.hasPendingRequest();
		}
		
		@Override
		public boolean canAcceptCygnusItem() {
			return true;
		}
		
		@Nullable
		@Override
		public Object giveItemToCygnus() {
			CorporeaRequest request = CorporeaHelper2.getCorporeaRequestInRetainer(retainer);
			if(request != null) {
				CorporeaHelper2.clearRetainer(retainer);
				return request;
			} else return null;
		}
		
		@Override
		public void acceptItemFromCygnus(Object item) {
			if(item instanceof CorporeaRequest) {
				CorporeaHelper2.setCorporeaRequestInRetainer(retainer, (CorporeaRequest) item);
			}
		}
	}
	
	private static final class CorporeaCrystalCubeFunnelable implements ICygnusFunnelable {
		public CorporeaCrystalCubeFunnelable(TileCorporeaCrystalCube crystalCube) {
			this.crystalCube = crystalCube;
		}
		
		private final TileCorporeaCrystalCube crystalCube;
		
		@Override
		public boolean canGiveCygnusItem() {
			return !crystalCube.getRequestTarget().isEmpty();
		}

		@Override
		public boolean canAcceptCygnusItem() {
			return true;
		}

		@Nullable
		@Override
		public Object giveItemToCygnus() {
			ItemStack item = crystalCube.getRequestTarget();
			//Is this needed
			ItemStack item2 = item.copy();
			item2.setCount(1);
			int count = crystalCube.getItemCount();
			
			return new CorporeaRequest(item2, true, count);
		}

		@Override
		public void acceptItemFromCygnus(Object item) {
			if(item instanceof CorporeaRequest) {
				CorporeaRequest request = (CorporeaRequest) item;
				if(request.matcher instanceof ItemStack) {
					crystalCube.setRequestTarget((ItemStack) request.matcher);
				}
			}
		}
	}
	
	//a helper used for the item entity & item frame funnelables since they are similar
	private static abstract class AbstractItemStackFunnelable implements ICygnusFunnelable {
		protected abstract ItemStack getStack();
		protected abstract void setStack(ItemStack stack);
		
		private ItemCygnusTicket asTicket() {
			//returning the casted item and nullchecking, is a lot less typing and get-this-get-thatting
			Item i = getStack().getItem();
			if(i instanceof ItemCygnusTicket) return (ItemCygnusTicket) i;
			else return null;
		}
		
		@Override
		public boolean canGiveCygnusItem() {
			ItemCygnusTicket ticket = asTicket();
			
			if(ticket != null) {
				return ticket.hasCygnusItem(getStack());
			} else {
				return !getStack().isEmpty();
			}
		}
		
		@Override
		public boolean canAcceptCygnusItem() {
			//can only accept items if it's a ticket to be written to
			return asTicket() != null;
		}
		
		@Nullable
		@Override
		public Object giveItemToCygnus() {
			ItemStack stack = getStack();
			ItemCygnusTicket ticket = asTicket();
			
			if(ticket != null) {
				//already checked has in canGive, so this should be a-ok
				Object ret = ticket.getCygnusItem(stack);
				ticket.clearCygnusItem(stack);
				return ret;
			} else {
				//it's not a ticket, so submit the item as a corporea request
				ItemStack stackCopy = stack.copy();
				int count = stackCopy.getCount();
				stackCopy.setCount(1);
				return new CorporeaRequest(stackCopy, true, count);
			}
		}
		
		@Override
		public void acceptItemFromCygnus(Object item) {
			ItemCygnusTicket ticket = asTicket();
			if(ticket != null) {
				ItemStack ticketStack = getStack();
				ticket.setCygnusItem(ticketStack, item);
				setStack(ticketStack);
			}
		}
	}
	
	private static final class ItemEntityFunnelable extends AbstractItemStackFunnelable {
		public ItemEntityFunnelable(EntityItem ent) {
			this.ent = ent;
		}
		
		private final EntityItem ent;
		
		@Override
		protected ItemStack getStack() {
			return ent.getItem/*Stack*/();
		}
		
		@Override
		protected void setStack(ItemStack stack) {
			ent.setItem(stack);
		}
	}
	
	private static final class ItemFrameFunnelable extends AbstractItemStackFunnelable {
		public ItemFrameFunnelable(EntityItemFrame frame) {
			this.frame = frame;
		}
		
		private final EntityItemFrame frame;
		
		@Override
		protected ItemStack getStack() {
			return frame.getDisplayedItem();
		}
		
		@Override
		protected void setStack(ItemStack stack) {
			frame.setDisplayedItem(stack);
		}
	}
}
