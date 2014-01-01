package appeng.parts.misc;

import java.util.EnumSet;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.api.AEApi;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPartCollsionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.util.AECableType;
import appeng.client.texture.CableBusTextures;
import appeng.me.helpers.AENetworkProxy;
import appeng.parts.PartBasicState;

public class PartToggleBus extends PartBasicState
{

	AENetworkProxy outerProxy = new AENetworkProxy( this, "outer", true );
	IGridConnection connection;

	protected final int REDSTONE_FLAG = 4;

	boolean hasRedstone = false;

	@Override
	protected int populateFlags(int cf)
	{
		return cf | (getIntention() ? REDSTONE_FLAG : 0);
	}

	@Override
	public void onNeighborChanged()
	{
		boolean oldHasRedstone = hasRedstone;
		hasRedstone = getHost().hasRedstone( side );

		if ( hasRedstone != oldHasRedstone )
		{
			updateInternalState();
			getHost().markForUpdate();
		}
	}

	public PartToggleBus(ItemStack is) {
		this( PartToggleBus.class, is );
	}

	public PartToggleBus(Class cls, ItemStack is) {
		super( cls, is );
	}

	@Override
	public void setPartHostInfo(ForgeDirection side, IPartHost host, TileEntity tile)
	{
		super.setPartHostInfo( side, host, tile );
		outerProxy.setValidSides( EnumSet.of( side ) );
	}

	@Override
	public void readFromNBT(NBTTagCompound extra)
	{
		super.readFromNBT( extra );
		outerProxy.readFromNBT( extra );
	}

	@Override
	public void writeToNBT(NBTTagCompound extra)
	{
		super.writeToNBT( extra );
		outerProxy.writeToNBT( extra );
	}

	@Override
	public void addToWorld()
	{
		super.addToWorld();
		outerProxy.onReady();
		updateInternalState();
	}

	private void updateInternalState()
	{
		boolean intention = getIntention();
		if ( intention != (connection != null) )
		{
			if ( proxy.getNode() != null && outerProxy.getNode() != null )
			{
				if ( intention )
				{
					connection = AEApi.instance().createGridConnection( proxy.getNode(), outerProxy.getNode() );
				}
				else
				{
					connection.destroy();
					connection = null;
				}
			}
		}
	}

	protected boolean getIntention()
	{
		return getHost().hasRedstone( side );
	}

	@Override
	public void removeFromWorld()
	{
		super.removeFromWorld();
		outerProxy.invalidate();
	}

	@Override
	public IGridNode getExternalFacingNode()
	{
		return outerProxy.getNode();
	}

	@Override
	public AECableType getCableConnectionType(ForgeDirection dir)
	{
		return AECableType.GLASS;
	}

	@Override
	public void setColors(boolean hasChan, boolean hasPower)
	{
		hasRedstone = (clientFlags & REDSTONE_FLAG) == REDSTONE_FLAG;
		super.setColors( hasChan && hasRedstone, hasPower && hasRedstone );
	}

	@Override
	public void renderStatic(int x, int y, int z, IPartRenderHelper rh, RenderBlocks renderer)
	{
		rh.useSimpliedRendering( x, y, z, this );
		rh.setTexture( is.getIconIndex() );

		rh.setBounds( 6, 6, 14, 10, 10, 16 );
		rh.renderBlock( x, y, z, renderer );

		rh.setBounds( 6, 6, 11, 10, 10, 13 );
		rh.renderBlock( x, y, z, renderer );

		rh.setTexture( CableBusTextures.PartMonitorSidesStatus.getIcon(), CableBusTextures.PartMonitorSidesStatus.getIcon(),
				CableBusTextures.PartMonitorBack.getIcon(), is.getIconIndex(), CableBusTextures.PartMonitorSidesStatus.getIcon(),
				CableBusTextures.PartMonitorSidesStatus.getIcon() );

		rh.setBounds( 6, 6, 13, 10, 10, 14 );
		rh.renderBlock( x, y, z, renderer );

		renderLights( x, y, z, rh, renderer );
	}

	@Override
	public void getBoxes(IPartCollsionHelper bch)
	{
		bch.addBox( 6, 6, 11, 10, 10, 16 );
	}

	@Override
	public void renderInventory(IPartRenderHelper rh, RenderBlocks renderer)
	{
		GL11.glTranslated( -0.2, -0.3, 0.0 );

		rh.setTexture( is.getIconIndex() );
		rh.setBounds( 6, 6, 14 - 4, 10, 10, 16 - 4 );
		rh.renderInventoryBox( renderer );

		rh.setBounds( 6, 6, 11 - 4, 10, 10, 13 - 4 );
		rh.renderInventoryBox( renderer );

		rh.setBounds( 6, 6, 13 - 4, 10, 10, 14 - 4 );
		rh.setTexture( CableBusTextures.PartMonitorSidesStatus.getIcon() );
		rh.renderInventoryBox( renderer );

		rh.setTexture( CableBusTextures.PartMonitorSidesStatusLights.getIcon() );
		rh.setInvColor( 0x000000 );
		rh.renderInventoryBox( renderer );
		rh.setInvColor( 0xffffff );

		rh.setTexture( null );
	}

	@Override
	public int cableConnectionRenderTo()
	{
		return 5;
	}
}
