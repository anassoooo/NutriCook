import { useRef } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import { Sphere, MeshDistortMaterial, Float } from '@react-three/drei'
import type { Mesh } from 'three'

interface OrbProps { pct: number }

function DistortOrb({ pct }: OrbProps) {
  const meshRef = useRef<Mesh>(null)

  useFrame(({ clock }) => {
    if (!meshRef.current) return
    meshRef.current.rotation.y = clock.getElapsedTime() * 0.25
    meshRef.current.rotation.x = Math.sin(clock.getElapsedTime() * 0.18) * 0.12
  })

  // green → amber → red based on how close to goal
  const color = pct < 0.6 ? '#16a34a' : pct < 0.9 ? '#f59e0b' : '#ef4444'

  return (
    <Float speed={1.8} rotationIntensity={0.2} floatIntensity={0.6}>
      <Sphere ref={meshRef} args={[1.3, 64, 64]}>
        <MeshDistortMaterial
          color={color}
          distort={0.28}
          speed={2}
          roughness={0.15}
          metalness={0.3}
          emissive={color}
          emissiveIntensity={0.15}
        />
      </Sphere>
    </Float>
  )
}

export default function NutritionOrb({ pct }: OrbProps) {
  return (
    <Canvas camera={{ position: [0, 0, 4.2], fov: 45 }} dpr={[1, 2]}>
      <ambientLight intensity={1.2} />
      <directionalLight position={[4, 4, 4]} intensity={1.5} color="#ffffff" />
      <pointLight position={[-3, -2, 2]} intensity={0.6} color="#bbf7d0" />
      <DistortOrb pct={pct} />
    </Canvas>
  )
}
