import { useRef, useMemo } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import { Float, Stars } from '@react-three/drei'
import type { Mesh } from 'three'
import * as THREE from 'three'

const COLORS = ['#4ade80', '#60a5fa', '#f472b6', '#facc15', '#fb923c']
const SHAPES: Array<'sphere' | 'box' | 'torus'> = ['sphere', 'box', 'torus']

function Particle({ idx }: { idx: number }) {
  const meshRef = useRef<Mesh>(null)
  const color = COLORS[idx % COLORS.length]
  const shape = SHAPES[idx % SHAPES.length]
  const pos = useMemo(
    () => new THREE.Vector3(
      (Math.random() - 0.5) * 8,
      (Math.random() - 0.5) * 5,
      (Math.random() - 0.5) * 4
    ),
    []
  )
  const speed = 0.3 + Math.random() * 0.5

  useFrame(({ clock }) => {
    if (!meshRef.current) return
    meshRef.current.rotation.x = clock.getElapsedTime() * speed
    meshRef.current.rotation.y = clock.getElapsedTime() * speed * 0.7
  })

  return (
    <Float speed={1 + Math.random()} floatIntensity={0.8} rotationIntensity={0.4}>
      <mesh ref={meshRef} position={pos}>
        {shape === 'sphere' && <sphereGeometry args={[0.18, 16, 16]} />}
        {shape === 'box' && <boxGeometry args={[0.28, 0.28, 0.28]} />}
        {shape === 'torus' && <torusGeometry args={[0.18, 0.07, 12, 24]} />}
        <meshStandardMaterial
          color={color}
          emissive={color}
          emissiveIntensity={0.4}
          roughness={0.2}
          metalness={0.5}
        />
      </mesh>
    </Float>
  )
}

export default function FoodParticles() {
  return (
    <Canvas camera={{ position: [0, 0, 6], fov: 50 }} dpr={[1, 2]}>
      <ambientLight intensity={0.3} />
      <pointLight position={[4, 4, 4]} intensity={1.2} color="#86efac" />
      <Stars radius={60} depth={40} count={800} factor={2} fade />
      {Array.from({ length: 18 }, (_, i) => <Particle key={i} idx={i} />)}
    </Canvas>
  )
}
