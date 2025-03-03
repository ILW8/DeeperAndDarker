package com.kyanite.deeperdarker.content.blocks.vegetation;

import com.kyanite.deeperdarker.content.DDBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class SculkVinesBlock extends GrowingPlantHeadBlock {
    private static final VoxelShape SHAPE = Block.box(4, 9, 4, 12, 16, 12);

    public SculkVinesBlock(Properties pProperties) {
        super(pProperties, Direction.DOWN, SHAPE, false, 0.1);
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(RandomSource pRandom) {
        return pRandom.nextIntBetweenInclusive(1, 2);
    }

    @Override
    protected @NotNull Block getBodyBlock() {
        return DDBlocks.SCULK_VINES_PLANT;
    }

    @Override
    protected boolean canGrowInto(BlockState pState) {
        return pState.isAir();
    }
}
