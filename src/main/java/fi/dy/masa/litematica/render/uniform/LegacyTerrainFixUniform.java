package fi.dy.masa.litematica.render.uniform;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

@ApiStatus.Internal
@VisibleForTesting
@Deprecated
public class LegacyTerrainFixUniform implements AutoCloseable
{
//	private static final int UBO_SIZE = new Std140SizeCalculator().putIVec2().putFloat().putInt().putInt().get();
//	private final MappableRingBuffer ubo;
//
//	public LegacyTerrainFixUniform()
//	{
//		this.ubo = new MappableRingBuffer(() -> MaLiLibReference.MOD_NAME + " LegacyTerrainFix UBO", 130, UBO_SIZE);
//
//		try (MemoryStack stack = MemoryStack.stackPush())
//		{
//			ByteBuffer buffer = stack.malloc(UBO_SIZE);
//			this.fillBuffer(buffer, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, Float.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
//		}
//	}
//
//	/**
//	 * Update the UBO Buffer
//	 *
//	 * @param atlasWidth      ()
//	 * @param atlasHeight     ()
//	 * @param chunkVisibility ()
//	 */
//	public void updateBuffer(int atlasWidth, int atlasHeight, float chunkVisibility)
//			throws IllegalArgumentException
//	{
//		if (atlasWidth <= 0 || atlasHeight <= 0)
//		{
//			throw new IllegalArgumentException("atlasWidth and atlasHeight must be positive");
//		}
//
//		final int useRGSS = Minecraft.getInstance().options.textureFiltering().get() == TextureFilteringMethod.RGSS ? 1 : 0;
//		final int hasShadersOn = IrisCompat.isShaderActive() ? 1 : 0;
//
//		try (GpuBufferSlice.MappedView mappedView = this.ubo.currentBuffer().map(false, true))
//		{
//			this.fillBuffer(mappedView.data(), 0, atlasWidth, atlasHeight, chunkVisibility, useRGSS, hasShadersOn);
//		}
//	}
//
//	public void fillBuffer(final ByteBuffer buffer, final int offset, int atlasWidth, int atlasHeight, float chunkVisibility, int useRGSS, int hasShadersOn)
//	{
//		buffer.position(offset);
//		Std140Builder.intoBuffer(buffer).putIVec2(atlasWidth, atlasHeight).putFloat(chunkVisibility).putInt(useRGSS).putInt(hasShadersOn);
//	}
//
//	/**
//	 * Draw the UBO buffer to a render pass
//	 *
//	 * @param pass ()
//	 */
//	public void drawPass(@Nonnull RenderPass pass)
//	{
//		pass.setUniform("LegacyTerrainFix", this.getCurrentBufferSlice());
//	}
//
//	/**
//	 * Get the 'currentBuffer' from the Ring Buffer.
//	 *
//	 * @return ()
//	 */
//	public GpuBufferSlice getCurrentBufferSlice()
//	{
//		return this.ubo.currentBuffer().slice(0L, UBO_SIZE);
//	}
//
//	/**
//	 * Call at the end if the Frame Pass {@link RenderSystem} flipFrame()
//	 */
//	public void endFrame()
//	{
//		this.ubo.rotate();
//	}
//
	@Override
	public void close() throws Exception
	{
//		this.ubo.close();
	}
}
